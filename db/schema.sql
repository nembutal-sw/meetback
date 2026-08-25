-- =========================================
-- meetBack 프로젝트 테이블 생성 스크립트 (MySQL)
-- DB: meet_back
-- =========================================

-- ============================================================
-- 1. USERS
-- ============================================================

CREATE TABLE users (
                       user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       email VARCHAR(255),
                       nickname VARCHAR(255),
                       password_hash VARCHAR(255),
    -- JWT 로그아웃 시 기존 Access / Refresh Token 무효화용
                       token_version BIGINT NOT NULL DEFAULT 0,
                       role VARCHAR(255) DEFAULT "USER",
                       deleted_at DATETIME,
                       created_at DATETIME,
                       updated_at DATETIME,

                       CONSTRAINT uq_users_email
                           UNIQUE (email)

                        CONSTRAINT uq_users_nickname
                            UNIQUE (nickname)
);


-- ============================================================
-- 2. SOCIAL
-- ============================================================

CREATE TABLE social (
                        social_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT,
                        provider VARCHAR(255),
                        provider_id VARCHAR(255),
                        email VARCHAR(255),
                        email_verified BOOLEAN,
                        name VARCHAR(255),

                        CONSTRAINT uq_social_user_provider
                            UNIQUE (user_id, provider),

                        CONSTRAINT uq_social_provider_provider_id
                            UNIQUE (provider, provider_id),

                        CONSTRAINT fk_social_user
                            FOREIGN KEY (user_id)
                                REFERENCES users(user_id)
                                ON DELETE CASCADE
);


-- ============================================================
-- 3. REFRESH_TOKENS
-- ============================================================

CREATE TABLE refresh_tokens (
                                refresh_token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id BIGINT,
                                token_hash VARCHAR(500),
                                expires_at DATETIME,
                                created_at DATETIME,
                                updated_at DATETIME,

                                CONSTRAINT uq_refresh_tokens_user
                                    UNIQUE (user_id),

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(user_id)
                                        ON DELETE CASCADE
);


-- ============================================================
-- 4. FEEDS
-- ============================================================

CREATE TABLE feeds (
                       feed_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       user_id BIGINT,
                       title VARCHAR(255),
                       content TEXT,
                       deleted_at DATETIME,
                       created_at DATETIME,
                       updated_at DATETIME,

                       CONSTRAINT fk_feeds_user
                           FOREIGN KEY (user_id)
                               REFERENCES users(user_id)
                               ON DELETE RESTRICT
);


-- ============================================================
-- 5. FEED_IMAGES
-- ============================================================

CREATE TABLE feed_images (
                             feed_image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             feed_id BIGINT,
                             image_url VARCHAR(255),
                             original_name VARCHAR(255),
                             stored_name VARCHAR(255),
                             sort_order INT,
                             created_at DATETIME,

                             CONSTRAINT fk_feed_images_feed
                                 FOREIGN KEY (feed_id)
                                     REFERENCES feeds(feed_id)
                                     ON DELETE CASCADE
);


-- ============================================================
-- 6. COMMENTS
-- ============================================================

CREATE TABLE comments (
                          comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          feed_id BIGINT,
                          user_id BIGINT,
                          content TEXT,
                          deleted_at DATETIME,
                          created_at DATETIME,
                          updated_at DATETIME,

                          CONSTRAINT fk_comments_feed
                              FOREIGN KEY (feed_id)
                                  REFERENCES feeds(feed_id)
                                  ON DELETE CASCADE,

                          CONSTRAINT fk_comments_user
                              FOREIGN KEY (user_id)
                                  REFERENCES users(user_id)
                                  ON DELETE RESTRICT
);


-- ============================================================
-- 7. FEED_LIKES
-- ============================================================

CREATE TABLE feed_likes (
                            like_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            feed_id BIGINT,
                            user_id BIGINT,
                            created_at DATETIME,

                            CONSTRAINT uq_feed_likes_feed_user
                                UNIQUE (feed_id, user_id),

                            CONSTRAINT fk_feed_likes_feed
                                FOREIGN KEY (feed_id)
                                    REFERENCES feeds(feed_id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_feed_likes_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(user_id)
                                    ON DELETE CASCADE
);


-- ============================================================
-- 8. TERMS
-- ============================================================

CREATE TABLE terms (
                       term_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       term_code VARCHAR(255),
                       term_name VARCHAR(255),
                       kakao_tag VARCHAR(255),
                       required BOOLEAN,
                       active BOOLEAN,
                       created_at DATETIME,
                       updated_at DATETIME,

                       CONSTRAINT uq_terms_term_code
                           UNIQUE (term_code)
);


-- ============================================================
-- 9. USER_TERM_AGREEMENTS
-- ============================================================

CREATE TABLE user_term_agreements (
                                      agreement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      user_id BIGINT,
                                      term_id BIGINT,
                                      agreed BOOLEAN,
                                      agreed_at DATETIME,
                                      revoked_at DATETIME,
                                      created_at DATETIME,

                                      CONSTRAINT uq_user_term_agreement
                                          UNIQUE (user_id, term_id),

                                      CONSTRAINT fk_user_term_agreement_user
                                          FOREIGN KEY (user_id)
                                              REFERENCES users(user_id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT fk_user_term_agreement_term
                                          FOREIGN KEY (term_id)
                                              REFERENCES terms(term_id)
                                              ON DELETE RESTRICT
);


-- ============================================================
-- 10. MEETINGS
-- ============================================================

CREATE TABLE meetings (
                          meeting_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          host_user_id BIGINT NOT NULL,
                          title VARCHAR(100) NOT NULL,

                          status VARCHAR(20) NOT NULL DEFAULT 'INPUT_OPEN',

                          desired_end_at DATETIME NOT NULL,
                          calculation_version INT NOT NULL DEFAULT 0,

                          invite_code VARCHAR(50) UNIQUE,
                          final_candidate_id BIGINT,

                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP,

                          CONSTRAINT fk_meetings_host_user
                              FOREIGN KEY (host_user_id)
                                  REFERENCES users(user_id)
                                  ON DELETE RESTRICT
);


-- ============================================================
-- 11. MEETING_PARTICIPANTS
-- ============================================================

CREATE TABLE meeting_participants (
                                      participant_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                      meeting_id BIGINT NOT NULL,
                                      user_id BIGINT NOT NULL,

                                      input_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

                                      departure_name VARCHAR(100),
                                      departure_address VARCHAR(255),
                                      departure_latitude DECIMAL(10,7),
                                      departure_longitude DECIMAL(10,7),

                                      return_name VARCHAR(100),
                                      return_address VARCHAR(255),
                                      return_latitude DECIMAL(10,7),
                                      return_longitude DECIMAL(10,7),

                                      submitted_at DATETIME,

                                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,

                                      CONSTRAINT uq_meeting_participant
                                          UNIQUE (meeting_id, user_id),

                                      CONSTRAINT fk_participant_meeting
                                          FOREIGN KEY (meeting_id)
                                              REFERENCES meetings(meeting_id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT fk_participant_user
                                          FOREIGN KEY (user_id)
                                              REFERENCES users(user_id)
                                              ON DELETE RESTRICT
);


-- ============================================================
-- 12. MEETING_CANDIDATES
-- ============================================================

CREATE TABLE meeting_candidates (
                                    candidate_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                    meeting_id BIGINT NOT NULL,
                                    proposer_participant_id BIGINT NOT NULL,

                                    place_name VARCHAR(100) NOT NULL,
                                    address VARCHAR(255) NOT NULL,
                                    latitude DECIMAL(10,7) NOT NULL,
                                    longitude DECIMAL(10,7) NOT NULL,

                                    is_active BOOLEAN NOT NULL DEFAULT TRUE,

                                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,

                                    CONSTRAINT uq_meeting_candidate_proposer
                                        UNIQUE (meeting_id, proposer_participant_id),

                                    CONSTRAINT fk_candidate_meeting
                                        FOREIGN KEY (meeting_id)
                                            REFERENCES meetings(meeting_id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT fk_candidate_proposer
                                        FOREIGN KEY (proposer_participant_id)
                                            REFERENCES meeting_participants(participant_id)
                                            ON DELETE CASCADE
);


-- ============================================================
-- 13. CANDIDATE_RETURN_RESULTS
-- ============================================================

CREATE TABLE candidate_return_results (
                                          result_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                          meeting_id BIGINT NOT NULL,
                                          candidate_id BIGINT NOT NULL,
                                          participant_id BIGINT NOT NULL,

                                          calculation_version INT NOT NULL,

                                          return_minutes INT,
                                          transfer_count INT,

                                          last_train_departure_at DATETIME,
                                          last_train_arrival_at DATETIME,
                                          last_safe_departure_at DATETIME,

                                          can_return BOOLEAN NOT NULL,

                                          calculated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                          CONSTRAINT uq_candidate_return_result
                                              UNIQUE (
                                                      candidate_id,
                                                      participant_id,
                                                      calculation_version
                                                  ),

                                          CONSTRAINT fk_return_result_meeting
                                              FOREIGN KEY (meeting_id)
                                                  REFERENCES meetings(meeting_id)
                                                  ON DELETE CASCADE,

                                          CONSTRAINT fk_return_result_candidate
                                              FOREIGN KEY (candidate_id)
                                                  REFERENCES meeting_candidates(candidate_id)
                                                  ON DELETE CASCADE,

                                          CONSTRAINT fk_return_result_participant
                                              FOREIGN KEY (participant_id)
                                                  REFERENCES meeting_participants(participant_id)
                                                  ON DELETE CASCADE
);


-- ============================================================
-- 14. PLACE_VOTES
-- ============================================================

CREATE TABLE place_votes (
                             vote_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                             meeting_id BIGINT NOT NULL,
                             participant_id BIGINT NOT NULL,
                             candidate_id BIGINT NOT NULL,

                             vote_change_count INT NOT NULL DEFAULT 0,

                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP,

                             CONSTRAINT uq_meeting_participant_vote
                                 UNIQUE (meeting_id, participant_id),

                             CONSTRAINT fk_vote_meeting
                                 FOREIGN KEY (meeting_id)
                                     REFERENCES meetings(meeting_id)
                                     ON DELETE CASCADE,

                             CONSTRAINT fk_vote_participant
                                 FOREIGN KEY (participant_id)
                                     REFERENCES meeting_participants(participant_id)
                                     ON DELETE CASCADE,

                             CONSTRAINT fk_vote_candidate
                                 FOREIGN KEY (candidate_id)
                                     REFERENCES meeting_candidates(candidate_id)
                                     ON DELETE CASCADE
);


-- ============================================================
-- 15. CHAT_MESSAGES
-- ============================================================

CREATE TABLE chat_messages (
                               message_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                               meeting_id BIGINT NOT NULL,
                               participant_id BIGINT NULL,

                               message_type VARCHAR(20) NOT NULL,
                               event_type VARCHAR(50) NULL,

                               content VARCHAR(1000) NOT NULL,

                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_chat_meeting
                                   FOREIGN KEY (meeting_id)
                                       REFERENCES meetings(meeting_id)
                                       ON DELETE CASCADE,

                               CONSTRAINT fk_chat_participant
                                   FOREIGN KEY (participant_id)
                                       REFERENCES meeting_participants(participant_id)
                                       ON DELETE SET NULL
);


-- ============================================================
-- 16. CANDIDATE_EVALUATIONS
-- ============================================================

CREATE TABLE candidate_evaluations (
                                       evaluation_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                       candidate_id BIGINT NOT NULL,

                                       calculation_version INT NOT NULL,

                                       all_returnable BOOLEAN NOT NULL,

                                       deadline_at DATETIME NULL,

                                       golden_margin_minutes INT NULL,

                                       average_return_minutes DOUBLE NULL,

                                       fairness_gap_minutes INT NULL,

                                       fairness_score INT NULL,

                                       rule_score DOUBLE NULL,

                                       recommendation_rank INT NULL,

                                       updated_at DATETIME NOT NULL
                                           DEFAULT CURRENT_TIMESTAMP
                                           ON UPDATE CURRENT_TIMESTAMP,

                                       CONSTRAINT uq_candidate_evaluation
                                           UNIQUE (candidate_id),

                                       CONSTRAINT fk_evaluation_candidate
                                           FOREIGN KEY (candidate_id)
                                               REFERENCES meeting_candidates(candidate_id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT chk_evaluation_calculation_version
                                           CHECK (calculation_version >= 0),

                                       CONSTRAINT chk_average_return_minutes
                                           CHECK (
                                               average_return_minutes IS NULL
                                                   OR average_return_minutes >= 0
                                               ),

                                       CONSTRAINT chk_fairness_gap_minutes
                                           CHECK (
                                               fairness_gap_minutes IS NULL
                                                   OR fairness_gap_minutes >= 0
                                               ),

                                       CONSTRAINT chk_fairness_score
                                           CHECK (
                                               fairness_score IS NULL
                                                   OR fairness_score IN (0, 10, 20, 30, 40, 50)
                                               ),

                                       CONSTRAINT chk_recommendation_rank
                                           CHECK (
                                               recommendation_rank IS NULL
                                                   OR recommendation_rank >= 1
                                               )
);


-- ============================================================
-- 17. MEETINGS.final_candidate_id FK
--
-- meeting_candidates가 생성된 이후에 설정해야 함
-- ============================================================

ALTER TABLE meetings
    ADD CONSTRAINT fk_meetings_final_candidate
        FOREIGN KEY (final_candidate_id)
            REFERENCES meeting_candidates(candidate_id)
            ON DELETE SET NULL;
