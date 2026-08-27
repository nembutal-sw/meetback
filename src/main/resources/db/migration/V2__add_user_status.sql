-- 회원의 서비스 이용 가능 상태를 관리한다.
ALTER TABLE users
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' AFTER role,
    ADD CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED'));
