-- 최초 운영 계정은 기존 관리자나 로그인 ID 충돌이 없을 때만 생성한다.
INSERT INTO users (
    email,
    nickname,
    password_hash,
    token_version,
    role,
    status,
    created_at,
    updated_at
)
SELECT
    'admin',
    'admin',
    '$2a$10$eVpNzvl1FIHqR7h8BEXDY.t9QoKsfVCzH7kDm97eqemHdfVEhIbRG',
    0,
    'ADMIN',
    'ACTIVE',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE role IN ('ADMIN', 'ROLE_ADMIN')
)
AND NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'admin'
        OR nickname = 'admin'
);
