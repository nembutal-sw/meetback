// 서버에서 확인한 관리자 권한으로 운영 화면과 관리자 API를 연결한다.
(() => {
    "use strict";

    const api = {
        dashboard: "/api/admin/dashboard",
        users: "/api/admin/users",
        meetings: "/api/admin/meetings",
        accounts: "/api/admin/accounts",
        terms: "/api/admin/terms"
    };

    const view = {
        users: {
            query: "",
            status: "",
            page: 0,
            size: 10,
            total: 0
        },
        meetings: {
            query: "",
            page: 0,
            size: 10,
            total: 0
        },
        chats: {
            meetingId: null,
            page: 0,
            size: 20,
            total: 0
        }
    };

    const loaded = new Set();
    let admin = null;
    let user = null;
    let action = null;
    let account = null;
    let mutationRunning = false;

    function get(id) {
        return document.getElementById(id);
    }

    function text(value, fallback = "-") {
        if (value == null || String(value).trim() === "") {
            return fallback;
        }

        return String(value);
    }

    function number(value, fallback = "--") {
        const result = Number(value);
        return Number.isFinite(result)
            ? result.toLocaleString("ko-KR")
            : fallback;
    }

    function date(value) {
        return value
            ? MeetBack.formatDateTime(value)
            : "-";
    }

    function isAdmin(role) {
        const value = text(role, "").trim().toUpperCase();
        return value === "ADMIN" || value === "ROLE_ADMIN";
    }

    function maskEmail(value) {
        const email = text(value, "");
        const [name, domain] = email.split("@");

        if (!name || !domain) {
            return email || "-";
        }

        const visible = name.slice(0, Math.min(2, name.length));
        return `${visible}${"*".repeat(Math.max(2, name.length - visible.length))}@${domain}`;
    }

    function setState(id, message, type = "") {
        const element = get(id);
        if (!element) {
            return;
        }

        element.hidden = false;
        element.classList.remove("is-loading", "is-error");

        if (type === "loading") {
            element.classList.add("is-loading");
        }
        else if (type === "error") {
            element.classList.add("is-error");
        }

        element.textContent = message;
    }

    function hideState(id) {
        const element = get(id);
        if (element) {
            element.hidden = true;
        }
    }

    function showNotice(message, type = "") {
        const notice = get("adminNotice");
        notice.classList.remove("error", "success");

        if (type) {
            notice.classList.add(type);
        }

        notice.textContent = message;
        notice.hidden = false;
    }

    async function fetchJson(url, options = {}) {
        const response = await MeetBack.authenticatedFetch(url, options);

        if (!response.ok) {
            let message = `요청을 처리하지 못했습니다. (${response.status})`;

            try {
                const body = await response.json();
                message = body.message || body.error || message;
            }
            catch (error) {
                // JSON 본문이 없는 오류 응답은 상태 코드 문구를 사용한다.
            }

            throw new Error(message);
        }

        if (response.status === 204) {
            return null;
        }

        const body = await response.text();
        if (!body) {
            return null;
        }

        try {
            return JSON.parse(body);
        }
        catch (error) {
            return body;
        }
    }

    function pageData(data, state) {
        const items = Array.isArray(data)
            ? data
            : Array.isArray(data?.items)
                ? data.items
                : Array.isArray(data?.content)
                    ? data.content
                    : [];

        return {
            items,
            total: Number(data?.total ?? data?.totalElements ?? items.length) || 0,
            page: Number(data?.page ?? data?.number ?? state.page) || 0,
            size: Number(data?.size ?? state.size) || state.size
        };
    }

    function totalPages(state) {
        return Math.max(1, Math.ceil(state.total / state.size));
    }

    function updatePager(prefix, state) {
        const pages = totalPages(state);
        get(`${prefix}PageInfo`).textContent = `${state.page + 1} / ${pages}`;
        get(`${prefix}PrevButton`).disabled = state.page <= 0;
        get(`${prefix}NextButton`).disabled = state.page + 1 >= pages;
    }

    function cell(label, value) {
        const td = document.createElement("td");
        td.dataset.label = label;

        if (value instanceof Node) {
            td.appendChild(value);
        }
        else {
            td.textContent = text(value);
        }

        return td;
    }

    function button(label, name, id) {
        const element = document.createElement("button");
        element.type = "button";
        element.textContent = label;
        element.dataset.action = name;
        element.dataset.id = id;
        return element;
    }

    function statusValue(item, type) {
        if (type === "user") {
            if (item?.deletedAt) {
                return "DELETED";
            }
            if (item?.status) {
                return text(item.status, "UNKNOWN").toUpperCase();
            }
            if (item?.suspendedAt || item?.suspended === true || item?.active === false) {
                return "SUSPENDED";
            }
            return "ACTIVE";
        }

        if (item?.status) {
            return text(item.status, "UNKNOWN").toUpperCase();
        }

        return "UNKNOWN";
    }

    function statusLabel(value) {
        const labels = {
            ACTIVE: "활성",
            SUSPENDED: "정지",
            DELETED: "탈퇴",
            INPUT_OPEN: "입력 중",
            VOTING: "투표 중",
            COMPLETED: "완료",
            CONFIRMED: "장소 확정",
            CLOSED: "종료",
            DRAFT: "작성 중",
            SUBMITTED: "제출 완료",
            CANDIDATE: "후보 투표",
            ABSTAIN: "기권",
            USER: "일반",
            SYSTEM: "시스템",
            UNKNOWN: "확인 필요"
        };

        return labels[value] || text(value);
    }

    function statusClass(value) {
        if (["ACTIVE", "INPUT_OPEN", "SUBMITTED", "CONFIRMED"].includes(value)) {
            return "active";
        }
        if (["VOTING"].includes(value)) {
            return "open";
        }
        if (["SUSPENDED", "COMPLETED", "CLOSED", "DRAFT"].includes(value)) {
            return "suspended";
        }
        if (value === "DELETED") {
            return "deleted";
        }
        return "";
    }

    function badge(value) {
        const normalized = text(value, "UNKNOWN").toUpperCase();
        const element = document.createElement("span");
        element.className = `admin-status-badge ${statusClass(normalized)}`.trim();
        element.textContent = statusLabel(normalized);
        return element;
    }

    function count(source, keys) {
        for (const key of keys) {
            const value = source?.[key];
            if (typeof value === "number" || /^\d+$/.test(String(value ?? ""))) {
                return number(value);
            }
        }
        return "--";
    }

    function renderTrends(userTrend = [], meetingTrend = []) {
        const chart = get("trendChart");
        const days = Math.max(userTrend.length, meetingTrend.length);
        const values = [];

        for (let index = 0; index < days; index += 1) {
            values.push({
                date: userTrend[index]?.date ?? meetingTrend[index]?.date,
                users: Number(userTrend[index]?.count) || 0,
                meetings: Number(meetingTrend[index]?.count) || 0
            });
        }

        const max = Math.max(1, ...values.flatMap(item => [item.users, item.meetings]));
        chart.replaceChildren();

        values.forEach(item => {
            const day = document.createElement("div");
            day.className = "admin-trend-day";
            day.setAttribute("role", "listitem");
            day.setAttribute(
                "aria-label",
                `${text(item.date, "날짜 미상")}: 신규 회원 ${item.users}명, 신규 모임 ${item.meetings}건`
            );

            const bars = document.createElement("div");
            bars.className = "admin-trend-bars";
            bars.setAttribute("aria-hidden", "true");
            const userBar = document.createElement("i");
            const meetingBar = document.createElement("i");
            userBar.className = "users";
            meetingBar.className = "meetings";
            userBar.style.setProperty("--bar-height", String(item.users / max * 100));
            meetingBar.style.setProperty("--bar-height", String(item.meetings / max * 100));
            userBar.title = `신규 회원 ${item.users}명`;
            meetingBar.title = `신규 모임 ${item.meetings}건`;
            bars.append(userBar, meetingBar);

            const label = document.createElement("span");
            label.textContent = text(item.date, "-").slice(5).replace("-", ".");
            day.append(bars, label);
            chart.appendChild(day);
        });
    }

    async function loadDashboard() {
        setState("dashboardState", "운영 현황을 불러오는 중입니다.", "loading");
        get("dashboardContent").hidden = true;

        try {
            const data = await fetchJson(api.dashboard);
            const summary = data?.summary || data || {};

            get("summaryUsers").textContent = count(summary, ["totalUsers", "userCount", "users"]);
            get("summaryMeetings").textContent = count(summary, ["ongoingMeetings", "activeMeetings", "activeMeetingCount", "meetingsInProgress"]);
            get("summaryVotes").textContent = count(summary, ["todayVotes", "voteCount", "totalVotes"]);
            get("summaryChats").textContent = count(summary, ["totalChatMessages", "todayChats", "chatCount", "totalChats"]);
            renderTrends(data?.userTrend, data?.meetingTrend);

            const userAlert = count(summary, ["suspendedUsers", "pendingUsers", "usersNeedingAttention"]);
            const meetingAlert = count(summary, ["meetingsNeedingAttention", "stalledMeetings"]);

            get("summaryUserNote").textContent = userAlert === "--"
                ? "회원 목록에서 현재 계정 상태를 확인할 수 있습니다."
                : `확인이 필요한 회원 ${userAlert}명`;
            get("summaryMeetingNote").textContent = meetingAlert === "--"
                ? "모임 목록에서 진행 상태와 참가자를 확인할 수 있습니다."
                : `확인이 필요한 모임 ${meetingAlert}건`;

            hideState("dashboardState");
            get("dashboardContent").hidden = false;
            loaded.add("dashboard");
        }
        catch (error) {
            setState("dashboardState", error.message, "error");
        }
    }

    function renderUsers(items) {
        const body = get("usersTableBody");
        body.replaceChildren();

        items.forEach(item => {
            const id = item.userId ?? item.id;
            const copy = document.createElement("span");
            copy.className = "admin-account-copy";

            const nickname = document.createElement("strong");
            nickname.textContent = text(item.nickname, "이름 없음");
            const email = document.createElement("small");
            email.textContent = maskEmail(item.email);
            copy.append(nickname, email);

            const row = document.createElement("tr");
            row.append(
                cell("ID", id),
                cell("계정", copy),
                cell("권한", item.role),
                cell("상태", badge(statusValue(item, "user"))),
                cell("가입일", date(item.createdAt)),
                cell("확인", button("상세", "user-detail", id))
            );
            body.appendChild(row);
        });
    }

    async function loadUsers() {
        const state = view.users;
        const params = new URLSearchParams({
            page: state.page,
            size: state.size
        });

        if (state.query) {
            params.set("query", state.query);
        }
        if (state.status) {
            params.set("status", state.status);
        }

        setState("usersState", "회원 목록을 불러오는 중입니다.", "loading");
        get("usersTableWrap").hidden = true;

        try {
            const data = pageData(await fetchJson(`${api.users}?${params}`), state);
            Object.assign(state, {
                total: data.total,
                page: data.page,
                size: data.size
            });

            updatePager("users", state);
            loaded.add("users");

            if (data.items.length === 0) {
                setState("usersState", "조건에 맞는 회원이 없습니다.");
                return;
            }

            renderUsers(data.items);
            hideState("usersState");
            get("usersTableWrap").hidden = false;
        }
        catch (error) {
            setState("usersState", error.message, "error");
            updatePager("users", state);
        }
    }

    function detailRow(label, value) {
        const row = document.createElement("div");
        const dt = document.createElement("dt");
        const dd = document.createElement("dd");
        dt.textContent = label;
        dd.textContent = text(value);
        row.append(dt, dd);
        return row;
    }

    function renderUserDetail(item) {
        const list = get("userDetailList");
        const id = item.userId ?? item.id;
        const status = statusValue(item, "user");

        list.replaceChildren(
            detailRow("회원 ID", id),
            detailRow("닉네임", item.nickname),
            detailRow("이메일", item.email),
            detailRow("권한", item.role),
            detailRow("상태", statusLabel(status)),
            detailRow("가입일", date(item.createdAt)),
            detailRow("수정일", date(item.updatedAt)),
            detailRow("탈퇴일", date(item.deletedAt))
        );
        list.hidden = false;

        const statusButton = get("userStatusActionButton");
        const accountButton = get("userAccountEditButton");
        statusButton.hidden = true;
        accountButton.hidden = true;

        if (isAdmin(item.role) && status !== "DELETED") {
            accountButton.dataset.id = id;
            accountButton.hidden = false;
            return;
        }

        if (String(id) === String(admin?.userId)) {
            return;
        }

        if (status === "ACTIVE") {
            statusButton.textContent = "계정 정지";
            statusButton.dataset.action = "suspend";
            statusButton.hidden = false;
        }
        else if (status === "SUSPENDED") {
            statusButton.textContent = "계정 활성화";
            statusButton.dataset.action = "activate";
            statusButton.hidden = false;
        }
    }

    async function openUser(id) {
        const dialog = get("userDialog");
        get("userDetailList").hidden = true;
        get("userStatusActionButton").hidden = true;
        get("userAccountEditButton").hidden = true;
        setState("userDetailState", "회원 정보를 불러오는 중입니다.", "loading");

        if (!dialog.open) {
            dialog.showModal();
        }

        try {
            user = await fetchJson(`${api.users}/${encodeURIComponent(id)}`);
            renderUserDetail(user);
            hideState("userDetailState");
        }
        catch (error) {
            user = null;
            setState("userDetailState", error.message, "error");
        }
    }

    function openConfirm(title, message, label, danger, changes = []) {
        const dialog = get("confirmDialog");
        if (dialog.open || mutationRunning) {
            return false;
        }

        get("confirmDialogTitle").textContent = title;
        get("confirmDialogMessage").textContent = message;

        const list = get("confirmChangeList");
        list.replaceChildren(...changes.map(change => {
            const item = document.createElement("li");
            item.textContent = change;
            return item;
        }));
        list.hidden = changes.length === 0;

        const confirm = get("confirmActionButton");
        confirm.textContent = label;
        confirm.disabled = false;
        confirm.classList.toggle("danger-button", danger);
        confirm.classList.toggle("primary-button", !danger);
        dialog.showModal();
        return true;
    }

    function askUserAction(name) {
        if (!user || mutationRunning) {
            return;
        }

        const id = user.userId ?? user.id;
        const activate = name === "activate";
        const message = activate
            ? `${text(user.nickname, `회원 ${id}`)} 계정을 다시 활성화하시겠습니까?`
            : `${text(user.nickname, `회원 ${id}`)} 계정을 정지하시겠습니까? 로그인과 서비스 이용에 영향을 줄 수 있습니다.`;

        if (openConfirm("회원 상태 변경", message, activate ? "활성화" : "정지", !activate)) {
            action = { type: "user-status", id, name };
        }
    }

    async function updateUserStatus(pending) {
        await fetchJson(
            `${api.users}/${encodeURIComponent(pending.id)}/${pending.name}`,
            { method: "PATCH" }
        );

        const message = pending.name === "activate"
            ? "회원 계정을 활성화했습니다."
            : "회원 계정을 정지했습니다.";

        if (get("userDialog").open) {
            get("userDialog").close();
        }
        showNotice(message, "success");
        await Promise.all([loadUsers(), loadDashboard()]);
    }

    function clearAuthStorage() {
        ["accessToken", "refreshToken", "userId", "role", "nickname"].forEach(key => {
            localStorage.removeItem(key);
        });
    }

    function passwordIsValid(value) {
        return value.length >= 8
            && value.length <= 20
            && /[a-z]/.test(value)
            && /[A-Z]/.test(value)
            && /\d/.test(value)
            && /[^A-Za-z\d\s]/.test(value)
            && !/\s/.test(value);
    }

    async function openAccount(id) {
        const dialog = get("accountDialog");
        const self = String(id) === String(admin?.userId);

        account = null;
        get("accountForm").hidden = true;
        get("accountCurrentPassword").value = "";
        get("accountNewPassword").value = "";
        get("accountNewPasswordConfirm").value = "";
        get("accountTargetLabel").textContent = self
            ? "내 관리자 계정 정보를 변경합니다."
            : `관리자 #${id}의 계정 정보를 변경합니다.`;
        setState("accountState", "관리자 정보를 불러오는 중입니다.", "loading");

        if (!dialog.open) {
            dialog.showModal();
        }

        try {
            account = await fetchJson(`${api.accounts}/${encodeURIComponent(id)}`);
            get("accountLoginId").value = text(account?.loginId, "");
            get("accountNickname").value = text(account?.nickname, "");
            hideState("accountState");
            get("accountForm").hidden = false;
        }
        catch (error) {
            setState("accountState", error.message, "error");
        }
    }

    function askAccountUpdate(event) {
        event.preventDefault();
        if (!account || mutationRunning) {
            return;
        }

        const loginId = get("accountLoginId").value.trim().toLowerCase();
        const nickname = get("accountNickname").value.trim();
        const currentPassword = get("accountCurrentPassword").value;
        const newPassword = get("accountNewPassword").value;
        const newPasswordConfirm = get("accountNewPasswordConfirm").value;
        const changedLoginId = loginId !== text(account.loginId, "");
        const changedNickname = nickname !== text(account.nickname, "");

        if (!changedLoginId && !changedNickname && !newPassword) {
            setState("accountState", "변경할 로그인 ID, 닉네임 또는 새 비밀번호를 입력해주세요.", "error");
            return;
        }
        if (newPassword && !passwordIsValid(newPassword)) {
            setState("accountState", "새 비밀번호 정책을 확인해주세요.", "error");
            get("accountNewPassword").focus();
            return;
        }
        if (newPassword !== newPasswordConfirm) {
            setState("accountState", "새 비밀번호와 확인 값이 일치하지 않습니다.", "error");
            get("accountNewPasswordConfirm").focus();
            return;
        }

        const body = { currentPassword };
        if (changedLoginId) {
            body.loginId = loginId;
        }
        if (changedNickname) {
            body.nickname = nickname;
        }
        if (newPassword) {
            body.newPassword = newPassword;
            body.newPasswordConfirm = newPasswordConfirm;
        }

        const changes = [];
        if (changedLoginId) {
            changes.push(`로그인 ID: ${loginId}`);
        }
        if (changedNickname) {
            changes.push(`닉네임: ${nickname}`);
        }
        if (newPassword) {
            changes.push("비밀번호: 새 비밀번호로 변경");
        }

        hideState("accountState");
        const opened = openConfirm(
            "관리자 정보 변경",
            "아래 관리자 계정 정보를 변경하시겠습니까?",
            "변경",
            changedLoginId || Boolean(newPassword),
            changes
        );
        if (opened) {
            action = {
                type: "account-update",
                id: account.userId,
                body,
                changedNickname,
                nickname
            };
        }
    }

    async function updateAccount(pending) {
        setState("accountState", "관리자 정보를 변경하고 있습니다.", "loading");

        const result = await fetchJson(
            `${api.accounts}/${encodeURIComponent(pending.id)}`,
            {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(pending.body)
            }
        );

        if (result?.reLoginRequired) {
            sessionStorage.setItem(
                "authNotice",
                "로그인 정보가 변경되어 다시 로그인해주세요."
            );
            clearAuthStorage();
            window.location.replace("/login?reason=account-updated");
            return;
        }

        if (String(pending.id) === String(admin?.userId) && pending.changedNickname) {
            admin.nickname = pending.nickname;
            localStorage.setItem("nickname", pending.nickname);
            MeetBackSessionHeader.render(admin);
        }

        if (get("accountDialog").open) {
            get("accountDialog").close();
        }
        if (get("userDialog").open) {
            get("userDialog").close();
        }
        showNotice(result?.message || "관리자 정보를 변경했습니다.", "success");
        if (loaded.has("users")) {
            await loadUsers();
        }
    }

    async function runConfirmedAction() {
        if (!action || mutationRunning) {
            return;
        }

        const pending = action;
        action = null;
        mutationRunning = true;
        get("confirmActionButton").disabled = true;
        get("userStatusActionButton").disabled = true;
        get("accountSaveButton").disabled = true;
        get("termSubmitButton").disabled = true;
        get("confirmDialog").close();

        try {
            if (pending.type === "account-update") {
                await updateAccount(pending);
            }
            else if (pending.type === "user-status") {
                await updateUserStatus(pending);
            }
            else if (pending.type === "term-create") {
                await createTerm(pending);
            }
        }
        catch (error) {
            if (pending.type === "account-update") {
                setState("accountState", error.message, "error");
            }
            else if (pending.type === "term-create") {
                setState("termFormState", error.message, "error");
            }
            else {
                showNotice(error.message, "error");
            }
        }
        finally {
            mutationRunning = false;
            get("confirmActionButton").disabled = false;
            get("userStatusActionButton").disabled = false;
            get("accountSaveButton").disabled = false;
            get("termSubmitButton").disabled = false;
        }
    }

    function renderMeetings(items) {
        const body = get("meetingsTableBody");
        body.replaceChildren();

        items.forEach(item => {
            const id = item.meetingId ?? item.id;
            const title = text(item.title, `모임 ${id}`);
            const copy = document.createElement("span");
            copy.className = "admin-account-copy";

            const name = document.createElement("strong");
            name.textContent = title;
            const meta = document.createElement("small");
            meta.textContent = item.createdAt ? `생성 ${date(item.createdAt)}` : "생성일 정보 없음";
            copy.append(name, meta);

            const detail = button("상세", "meeting-detail", id);
            detail.dataset.title = title;

            const row = document.createElement("tr");
            row.append(
                cell("ID", id),
                cell("모임", copy),
                cell("상태", badge(statusValue(item, "meeting"))),
                cell("방장", item.hostNickname ?? item.hostUserId),
                cell("참가자", `${number(item.participantCount, "0")}명`),
                cell("확인", detail)
            );
            body.appendChild(row);
        });
    }

    async function loadMeetings() {
        const state = view.meetings;
        const params = new URLSearchParams({
            page: state.page,
            size: state.size
        });

        if (state.query) {
            params.set("query", state.query);
        }

        setState("meetingsState", "모임 목록을 불러오는 중입니다.", "loading");
        get("meetingsTableWrap").hidden = true;

        try {
            const data = pageData(await fetchJson(`${api.meetings}?${params}`), state);
            Object.assign(state, {
                total: data.total,
                page: data.page,
                size: data.size
            });

            updatePager("meetings", state);
            loaded.add("meetings");

            if (data.items.length === 0) {
                setState("meetingsState", "조건에 맞는 모임이 없습니다.");
                return;
            }

            renderMeetings(data.items);
            hideState("meetingsState");
            get("meetingsTableWrap").hidden = false;
        }
        catch (error) {
            setState("meetingsState", error.message, "error");
            updatePager("meetings", state);
        }
    }

    function renderParticipants(items) {
        const body = get("participantsTableBody");
        body.replaceChildren();

        items.forEach(item => {
            const row = document.createElement("tr");
            row.append(
                cell("참가자", item.participantId ?? item.id),
                cell("회원", item.userId),
                cell("닉네임", item.nickname),
                cell("입력 상태", badge(item.inputStatus)),
                cell("등록일", date(item.createdAt))
            );
            body.appendChild(row);
        });
    }

    function renderMeetingDetail(item) {
        const place = item.finalPlaceName
            ? item.finalPlaceName
            : "확정 전";
        const host = item.hostNickname
            ? `${item.hostNickname} (#${text(item.hostUserId)})`
            : item.hostUserId;

        get("meetingDetailList").replaceChildren(
            detailRow("모임 ID", item.meetingId ?? item.id),
            detailRow("모임명", item.title),
            detailRow("방장", host),
            detailRow("상태", statusLabel(statusValue(item, "meeting"))),
            detailRow("초대코드", item.inviteCode),
            detailRow("종료 희망", date(item.desiredEndAt)),
            detailRow("최종 장소", place),
            detailRow("최종 주소", item.finalAddress),
            detailRow("생성일", date(item.createdAt))
        );
        get("meetingDetailList").hidden = false;
    }

    async function openMeeting(id, title) {
        const dialog = get("meetingDialog");
        get("meetingDialogTitle").textContent = `${text(title, `모임 ${id}`)} 상세`;
        get("meetingDetailList").hidden = true;
        get("participantsTableWrap").hidden = true;
        setState("meetingDetailState", "모임 정보를 불러오는 중입니다.", "loading");
        setState("participantsState", "참가자 목록을 불러오는 중입니다.", "loading");

        if (!dialog.open) {
            dialog.showModal();
        }

        try {
            const [meeting, response] = await Promise.all([
                fetchJson(`${api.meetings}/${encodeURIComponent(id)}`),
                fetchJson(`${api.meetings}/${encodeURIComponent(id)}/participants`)
            ]);
            const data = pageData(
                response,
                { page: 0, size: 100 }
            );

            renderMeetingDetail(meeting);
            hideState("meetingDetailState");

            if (data.items.length === 0) {
                setState("participantsState", "등록된 참가자가 없습니다.");
                return;
            }

            renderParticipants(data.items);
            hideState("participantsState");
            get("participantsTableWrap").hidden = false;
        }
        catch (error) {
            setState("meetingDetailState", error.message, "error");
            setState("participantsState", error.message, "error");
        }
    }

    function renderCandidates(items) {
        const body = get("candidatesTableBody");
        body.replaceChildren();

        items.forEach(item => {
            const active = item.active ?? item.isActive;
            const row = document.createElement("tr");
            row.append(
                cell("후보", item.candidateId ?? item.id),
                cell("장소", item.placeName ?? item.name),
                cell("제안자", item.proposerNickname ?? item.proposerParticipantId),
                cell("상태", badge(active === false ? "CLOSED" : "ACTIVE"))
            );
            body.appendChild(row);
        });
    }

    function renderVotes(items) {
        const body = get("votesTableBody");
        body.replaceChildren();

        items.forEach(item => {
            const row = document.createElement("tr");
            row.append(
                cell("후보", item.candidateId ?? item.id),
                cell("장소", item.placeName ?? item.candidateName),
                cell("득표", `${number(item.voteCount, "0")}표`),
                cell("현황", Number(item.voteCount) > 0 ? "득표" : "0표")
            );
            body.appendChild(row);
        });
    }

    async function loadCandidates(id) {
        setState("candidatesState", "후보 장소를 불러오는 중입니다.", "loading");
        get("candidatesTableWrap").hidden = true;

        try {
            const data = pageData(
                await fetchJson(`${api.meetings}/${encodeURIComponent(id)}/candidates`),
                { page: 0, size: 100 }
            );
            get("candidatesCount").textContent = `${number(data.total, "0")}건`;

            if (data.items.length === 0) {
                setState("candidatesState", "등록된 후보 장소가 없습니다.");
                return;
            }

            renderCandidates(data.items);
            hideState("candidatesState");
            get("candidatesTableWrap").hidden = false;
        }
        catch (error) {
            setState("candidatesState", error.message, "error");
        }
    }

    async function loadVotes(id) {
        setState("votesState", "투표 현황을 불러오는 중입니다.", "loading");
        get("votesTableWrap").hidden = true;

        try {
            const response = await fetchJson(`${api.meetings}/${encodeURIComponent(id)}/votes`);
            const data = pageData(response?.candidates ?? response, { page: 0, size: 100 });
            get("votesCount").textContent = response?.totalParticipants == null
                ? `${number(data.total, "0")}건`
                : `${number(response.votedParticipants, "0")}/${number(response.totalParticipants, "0")}명 · 기권 ${number(response.abstainCount, "0")}`;

            if (data.items.length === 0) {
                setState("votesState", "등록된 투표가 없습니다.");
                return;
            }

            renderVotes(data.items);
            hideState("votesState");
            get("votesTableWrap").hidden = false;
        }
        catch (error) {
            setState("votesState", error.message, "error");
        }
    }

    function renderChats(items) {
        const body = get("chatsTableBody");
        body.replaceChildren();

        items.forEach(item => {
            const content = document.createElement("span");
            content.className = "admin-message-content";
            content.textContent = text(item.content);

            const row = document.createElement("tr");
            row.append(
                cell("ID", item.messageId ?? item.id),
                cell("작성자", item.nickname ?? item.participantId ?? "SYSTEM"),
                cell("메시지", content),
                cell("유형", badge(item.messageType)),
                cell("작성일", date(item.createdAt))
            );
            body.appendChild(row);
        });
    }

    async function loadChats() {
        const state = view.chats;
        if (!state.meetingId) {
            setState("chatsState", "모임 ID를 입력해 채팅 내역을 조회하세요.");
            return;
        }

        const params = new URLSearchParams({
            page: state.page,
            size: state.size
        });
        setState("chatsState", "채팅 내역을 불러오는 중입니다.", "loading");
        get("chatsTableWrap").hidden = true;

        try {
            const data = pageData(
                await fetchJson(
                    `${api.meetings}/${encodeURIComponent(state.meetingId)}/chats?${params}`
                ),
                state
            );
            Object.assign(state, {
                total: data.total,
                page: data.page,
                size: data.size
            });
            updatePager("chats", state);

            if (data.items.length === 0) {
                setState("chatsState", "저장된 채팅 메시지가 없습니다.");
                return;
            }

            renderChats(data.items);
            hideState("chatsState");
            get("chatsTableWrap").hidden = false;
        }
        catch (error) {
            setState("chatsState", error.message, "error");
            updatePager("chats", state);
        }
    }

    function termType(required) {
        const element = document.createElement("span");
        element.className = `admin-status-badge ${required ? "active" : "suspended"}`;
        element.textContent = required ? "필수" : "선택";
        return element;
    }

    function termActive(active) {
        const element = document.createElement("span");
        element.className = `admin-status-badge ${active ? "active" : "suspended"}`;
        element.textContent = active ? "적용 중" : "이전 버전";
        return element;
    }

    function termContent(content) {
        const details = document.createElement("details");
        details.className = "admin-term-details";

        const summary = document.createElement("summary");
        summary.textContent = "내용 보기";

        const body = document.createElement("pre");
        body.textContent = text(content, "약관 내용이 없습니다.");
        details.append(summary, body);
        return details;
    }

    function renderTerms(items) {
        const body = get("termsTableBody");
        body.replaceChildren();

        items.forEach(item => {
            const copy = document.createElement("span");
            copy.className = "admin-account-copy";

            const name = document.createElement("strong");
            name.textContent = text(item.termName, "이름 없음");
            const code = document.createElement("small");
            code.textContent = text(item.termCode, "코드 없음");
            copy.append(name, code);

            const row = document.createElement("tr");
            row.append(
                cell("ID", item.termId ?? item.id),
                cell("약관", copy),
                cell("버전", item.version),
                cell("구분", termType(Boolean(item.required))),
                cell("본문", termContent(item.content)),
                cell("적용 상태", termActive(Boolean(item.active))),
                cell("등록일", date(item.createdAt ?? item.effectiveAt))
            );
            body.appendChild(row);
        });
    }

    async function loadTerms() {
        setState("termsState", "약관 목록을 불러오는 중입니다.", "loading");
        get("termsTableWrap").hidden = true;

        try {
            const response = await fetchJson(api.terms);
            const items = Array.isArray(response)
                ? response
                : Array.isArray(response?.items)
                    ? response.items
                    : Array.isArray(response?.terms)
                        ? response.terms
                        : [];

            get("termsCount").textContent = `${number(items.length, "0")}건`;
            loaded.add("terms");

            if (items.length === 0) {
                setState("termsState", "등록된 약관 버전이 없습니다.");
                return;
            }

            renderTerms(items);
            hideState("termsState");
            get("termsTableWrap").hidden = false;
        }
        catch (error) {
            setState("termsState", error.message, "error");
        }
    }

    function askTermCreate(event) {
        event.preventDefault();
        if (mutationRunning) {
            return;
        }

        const body = {
            termCode: get("termCode").value.trim().toUpperCase(),
            termName: get("termName").value.trim(),
            version: get("termVersion").value.trim(),
            required: get("termRequired").value === "true",
            content: get("termContent").value.trim()
        };

        if (!body.termCode || !body.termName || !body.version || !body.content) {
            setState("termFormState", "약관 입력값을 모두 확인해주세요.", "error");
            return;
        }

        hideState("termFormState");
        const changes = [
            `약관 코드: ${body.termCode}`,
            `약관명: ${body.termName}`,
            `버전: ${body.version}`,
            `동의 구분: ${body.required ? "필수" : "선택"}`,
            `본문: ${body.content.length.toLocaleString("ko-KR")}자`
        ];

        if (openConfirm(
            "약관 새 버전 등록",
            "이 약관은 등록 즉시 회원가입 화면에 적용됩니다. 입력 내용을 확인해주세요.",
            "등록·적용",
            true,
            changes
        )) {
            action = { type: "term-create", body };
        }
    }

    async function createTerm(pending) {
        setState("termFormState", "약관 새 버전을 등록하고 있습니다.", "loading");

        const result = await fetchJson(
            api.terms,
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(pending.body)
            }
        );

        get("termForm").reset();
        hideState("termFormState");
        showNotice(result?.message || "약관 새 버전을 등록했습니다.", "success");
        await loadTerms();
    }

    function loadTab(name) {
        if (name === "users" && !loaded.has("users")) {
            loadUsers();
        }
        else if (name === "meetings" && !loaded.has("meetings")) {
            loadMeetings();
        }
        else if (name === "operations" && !loaded.has("terms")) {
            loadTerms();
        }
    }

    function activateTab(tab, focus = false) {
        const tabs = Array.from(document.querySelectorAll("[data-admin-tab]"));
        const panels = Array.from(document.querySelectorAll("[data-admin-panel]"));
        const name = tab.dataset.adminTab;

        tabs.forEach(item => {
            const active = item === tab;
            item.classList.toggle("active", active);
            item.setAttribute("aria-selected", String(active));
            item.tabIndex = active ? 0 : -1;
        });

        panels.forEach(panel => {
            panel.hidden = panel.dataset.adminPanel !== name;
        });

        if (focus) {
            tab.focus();
        }

        loadTab(name);
    }

    function bindTabs() {
        const list = document.querySelector(".admin-tab-list");
        const tabs = Array.from(list.querySelectorAll("[data-admin-tab]"));

        tabs.forEach(tab => {
            tab.addEventListener("click", () => activateTab(tab));
        });

        list.addEventListener("keydown", event => {
            const current = event.target.closest("[data-admin-tab]");
            const index = tabs.indexOf(current);

            if (index < 0) {
                return;
            }

            let next = index;
            if (["ArrowDown", "ArrowRight"].includes(event.key)) {
                next = (index + 1) % tabs.length;
            }
            else if (["ArrowUp", "ArrowLeft"].includes(event.key)) {
                next = (index - 1 + tabs.length) % tabs.length;
            }
            else if (event.key === "Home") {
                next = 0;
            }
            else if (event.key === "End") {
                next = tabs.length - 1;
            }
            else {
                return;
            }

            event.preventDefault();
            activateTab(tabs[next], true);
        });
    }

    function bindFilters() {
        get("usersFilter").addEventListener("submit", event => {
            event.preventDefault();
            Object.assign(view.users, {
                query: get("userQuery").value.trim(),
                status: get("userStatus").value,
                page: 0
            });
            loadUsers();
        });

        get("resetUsersButton").addEventListener("click", () => {
            get("usersFilter").reset();
            Object.assign(view.users, { query: "", status: "", page: 0 });
            loadUsers();
        });

        get("meetingsFilter").addEventListener("submit", event => {
            event.preventDefault();
            Object.assign(view.meetings, {
                query: get("meetingQuery").value.trim(),
                page: 0
            });
            loadMeetings();
        });

        get("resetMeetingsButton").addEventListener("click", () => {
            get("meetingsFilter").reset();
            Object.assign(view.meetings, { query: "", page: 0 });
            loadMeetings();
        });

        get("placesFilter").addEventListener("submit", event => {
            event.preventDefault();
            const id = Number(get("placesMeetingId").value);
            if (!Number.isInteger(id) || id <= 0) {
                showNotice("올바른 모임 ID를 입력해주세요.", "error");
                return;
            }
            Promise.all([loadCandidates(id), loadVotes(id)]);
        });

        get("chatsFilter").addEventListener("submit", event => {
            event.preventDefault();
            const id = Number(get("chatsMeetingId").value);
            if (!Number.isInteger(id) || id <= 0) {
                showNotice("올바른 모임 ID를 입력해주세요.", "error");
                return;
            }
            Object.assign(view.chats, { meetingId: id, page: 0 });
            loadChats();
        });
    }

    function bindTables() {
        get("usersTableBody").addEventListener("click", event => {
            const target = event.target.closest("button[data-action='user-detail']");
            if (target) {
                openUser(target.dataset.id);
            }
        });

        get("meetingsTableBody").addEventListener("click", event => {
            const target = event.target.closest("button[data-action='meeting-detail']");
            if (target) {
                openMeeting(target.dataset.id, target.dataset.title);
            }
        });
    }

    function bindPagination() {
        get("usersPrevButton").addEventListener("click", () => {
            view.users.page = Math.max(0, view.users.page - 1);
            loadUsers();
        });
        get("usersNextButton").addEventListener("click", () => {
            view.users.page += 1;
            loadUsers();
        });
        get("meetingsPrevButton").addEventListener("click", () => {
            view.meetings.page = Math.max(0, view.meetings.page - 1);
            loadMeetings();
        });
        get("meetingsNextButton").addEventListener("click", () => {
            view.meetings.page += 1;
            loadMeetings();
        });
        get("chatsPrevButton").addEventListener("click", () => {
            view.chats.page = Math.max(0, view.chats.page - 1);
            loadChats();
        });
        get("chatsNextButton").addEventListener("click", () => {
            view.chats.page += 1;
            loadChats();
        });
    }

    function bindDialogs() {
        document.querySelectorAll("[data-close-dialog]").forEach(buttonElement => {
            buttonElement.addEventListener("click", () => {
                get(buttonElement.dataset.closeDialog).close();
            });
        });

        get("userStatusActionButton").addEventListener("click", event => {
            askUserAction(event.currentTarget.dataset.action);
        });
        get("userAccountEditButton").addEventListener("click", event => {
            openAccount(event.currentTarget.dataset.id);
        });
        get("accountForm").addEventListener("submit", askAccountUpdate);
        get("termForm").addEventListener("submit", askTermCreate);
        get("termForm").addEventListener("reset", () => {
            hideState("termFormState");
        });
        get("confirmDialog").addEventListener("close", () => {
            if (!mutationRunning) {
                action = null;
            }
        });
        get("confirmActionButton").addEventListener("click", runConfirmedAction);
    }

    function bindEvents() {
        bindTabs();
        bindFilters();
        bindTables();
        bindPagination();
        bindDialogs();
        get("refreshDashboardButton").addEventListener("click", loadDashboard);
        get("adminAccountButton").addEventListener("click", () => openAccount(admin.userId));
    }

    async function initialize() {
        try {
            admin = await MeetBack.checkLogin();

            if (!admin) {
                return;
            }

            if (!isAdmin(admin.role)) {
                window.location.replace("/home");
                return;
            }

            MeetBackSessionHeader.render(admin);
            bindEvents();
            get("adminAccessState").hidden = true;
            get("adminPage").hidden = false;
            await loadDashboard();
        }
        catch (error) {
            get("adminAccessState").textContent = "관리자 화면을 준비하지 못했습니다.";
            console.error("[ADMIN INITIALIZE ERROR]", error);
        }
    }

    initialize();
})();
