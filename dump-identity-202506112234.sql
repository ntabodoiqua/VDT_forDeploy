-- MySQL dump 10.13  Distrib 8.0.19, for Win64 (x86_64)
--
-- Host: localhost    Database: identity
-- ------------------------------------------------------
-- Server version	8.4.5

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` varchar(255) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `created_by_id` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_46ccwnsi9409t36lurvtyljak` (`name`),
  KEY `FKohag53txblab85pis128h20ow` (`created_by_id`),
  CONSTRAINT `FKohag53txblab85pis128h20ow` FOREIGN KEY (`created_by_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course`
--

DROP TABLE IF EXISTS `course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course` (
  `id` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `detailed_description` longtext,
  `end_date` date DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `start_date` date DEFAULT NULL,
  `thumbnail_url` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `total_lessons` int NOT NULL,
  `instructor_id` varchar(255) DEFAULT NULL,
  `category_id` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `requires_approval` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKc0xls9e7uqc9o08ae0t2ywr6n` (`instructor_id`),
  KEY `FKkyes7515s3ypoovxrput029bh` (`category_id`),
  CONSTRAINT `FKc0xls9e7uqc9o08ae0t2ywr6n` FOREIGN KEY (`instructor_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKkyes7515s3ypoovxrput029bh` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_document`
--

DROP TABLE IF EXISTS `course_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_document` (
  `id` varchar(255) NOT NULL,
  `content_type` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_size` bigint NOT NULL,
  `original_file_name` varchar(255) NOT NULL,
  `title` varchar(255) NOT NULL,
  `uploaded_at` datetime(6) DEFAULT NULL,
  `course_id` varchar(255) NOT NULL,
  `uploaded_by` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKa0k32ev8uq2a8gc78vq2f076p` (`course_id`),
  KEY `FK8y126abifdk4ocqxevl4ppioc` (`uploaded_by`),
  CONSTRAINT `FK8y126abifdk4ocqxevl4ppioc` FOREIGN KEY (`uploaded_by`) REFERENCES `user` (`id`),
  CONSTRAINT `FKa0k32ev8uq2a8gc78vq2f076p` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_lesson`
--

DROP TABLE IF EXISTS `course_lesson`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_lesson` (
  `id` varchar(255) NOT NULL,
  `is_visible` bit(1) NOT NULL,
  `order_index` int NOT NULL,
  `course_id` varchar(255) DEFAULT NULL,
  `lesson_id` varchar(255) DEFAULT NULL,
  `prerequisite_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKf3no0dhg3wsy4h2sih25rivr2` (`course_id`),
  KEY `FKog5frd1fln5htfsdmsxc8o3jq` (`lesson_id`),
  KEY `FK1uf5l34syf3tiuysmsvel1tc8` (`prerequisite_id`),
  CONSTRAINT `FK1uf5l34syf3tiuysmsvel1tc8` FOREIGN KEY (`prerequisite_id`) REFERENCES `course_lesson` (`id`),
  CONSTRAINT `FKf3no0dhg3wsy4h2sih25rivr2` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`),
  CONSTRAINT `FKog5frd1fln5htfsdmsxc8o3jq` FOREIGN KEY (`lesson_id`) REFERENCES `lesson` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_review`
--

DROP TABLE IF EXISTS `course_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_review` (
  `id` varchar(255) NOT NULL,
  `comment` tinytext,
  `is_approved` bit(1) NOT NULL,
  `rating` int NOT NULL,
  `review_date` date DEFAULT NULL,
  `course_id` varchar(255) NOT NULL,
  `student_id` varchar(255) NOT NULL,
  `is_rejected` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1yqair0hyov3nur25i0pqp5vb` (`student_id`,`course_id`),
  KEY `FKsbrpx0jdl735ordw1woeyuvd1` (`course_id`),
  CONSTRAINT `FK23pnlajcmngxhrytgy5jxo7gj` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKsbrpx0jdl735ordw1woeyuvd1` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`),
  CONSTRAINT `course_review_chk_1` CHECK (((`rating` <= 5) and (`rating` >= 1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `document_view`
--

DROP TABLE IF EXISTS `document_view`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document_view` (
  `id` varchar(255) NOT NULL,
  `unique_key` varchar(255) DEFAULT NULL,
  `view_duration_seconds` bigint DEFAULT NULL,
  `viewed_at` datetime(6) DEFAULT NULL,
  `lesson_document_id` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKpfrfrbiuctyyvdxnqirvvr51h` (`user_id`,`lesson_document_id`),
  KEY `FKelsq7qikb56rmfqwx8icscius` (`lesson_document_id`),
  CONSTRAINT `FK2c5wow1mcwc03w6mxlencpwm` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKelsq7qikb56rmfqwx8icscius` FOREIGN KEY (`lesson_document_id`) REFERENCES `lesson_document` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `enrollment`
--

DROP TABLE IF EXISTS `enrollment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollment` (
  `id` varchar(255) NOT NULL,
  `completion_date` date DEFAULT NULL,
  `enrollment_date` date DEFAULT NULL,
  `is_completed` bit(1) NOT NULL,
  `progress` double NOT NULL,
  `course_id` varchar(255) DEFAULT NULL,
  `student_id` varchar(255) DEFAULT NULL,
  `approval_status` enum('PENDING','APPROVED','REJECTED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbhhcqkw1px6yljqg92m0sh2gt` (`course_id`),
  KEY `FK65os88xfjxr2tos3tksqeleg6` (`student_id`),
  CONSTRAINT `FK65os88xfjxr2tos3tksqeleg6` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKbhhcqkw1px6yljqg92m0sh2gt` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `invalidated_token`
--

DROP TABLE IF EXISTS `invalidated_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invalidated_token` (
  `id` varchar(255) NOT NULL,
  `expiry_date` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lesson`
--

DROP TABLE IF EXISTS `lesson`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lesson` (
  `id` varchar(255) NOT NULL,
  `attachment_url` varchar(255) DEFAULT NULL,
  `content` longtext,
  `created_at` datetime(6) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `video_url` varchar(255) DEFAULT NULL,
  `created_by_id` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKb2dj9eqr9lmb9ueyswsy3mfyc` (`created_by_id`),
  CONSTRAINT `FKb2dj9eqr9lmb9ueyswsy3mfyc` FOREIGN KEY (`created_by_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lesson_document`
--

DROP TABLE IF EXISTS `lesson_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lesson_document` (
  `id` varchar(255) NOT NULL,
  `content_type` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_size` bigint NOT NULL,
  `original_file_name` varchar(255) NOT NULL,
  `title` varchar(255) NOT NULL,
  `uploaded_at` datetime(6) DEFAULT NULL,
  `lesson_id` varchar(255) NOT NULL,
  `uploaded_by` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK5hfa65dccniwkthsswtn60vfy` (`lesson_id`),
  KEY `FK94q30u846wdvwu779gs9s2kr3` (`uploaded_by`),
  CONSTRAINT `FK5hfa65dccniwkthsswtn60vfy` FOREIGN KEY (`lesson_id`) REFERENCES `lesson` (`id`),
  CONSTRAINT `FK94q30u846wdvwu779gs9s2kr3` FOREIGN KEY (`uploaded_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permission`
--

DROP TABLE IF EXISTS `permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission` (
  `name` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `progress`
--

DROP TABLE IF EXISTS `progress`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `progress` (
  `id` varchar(255) NOT NULL,
  `completion_date` date DEFAULT NULL,
  `is_completed` bit(1) NOT NULL,
  `enrollment_id` varchar(255) DEFAULT NULL,
  `lesson_id` varchar(255) DEFAULT NULL,
  `quiz_score` double DEFAULT NULL,
  `completed_quiz_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkko4cc9b6v1k7sfjo0qp279wc` (`enrollment_id`),
  KEY `FKlwcrcj99o31ue6bqeyxvoo0kx` (`lesson_id`),
  KEY `FKr5ndqow0u2n92y755g0r9aert` (`completed_quiz_id`),
  CONSTRAINT `FKkko4cc9b6v1k7sfjo0qp279wc` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollment` (`id`),
  CONSTRAINT `FKlwcrcj99o31ue6bqeyxvoo0kx` FOREIGN KEY (`lesson_id`) REFERENCES `lesson` (`id`),
  CONSTRAINT `FKr5ndqow0u2n92y755g0r9aert` FOREIGN KEY (`completed_quiz_id`) REFERENCES `quiz` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `quiz`
--

DROP TABLE IF EXISTS `quiz`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz` (
  `id` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `end_time` datetime(6) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `max_attempts` int DEFAULT NULL,
  `passing_score` double DEFAULT NULL,
  `scoring_method` enum('HIGHEST','LATEST','AVERAGE') DEFAULT NULL,
  `show_correct_answers` bit(1) DEFAULT NULL,
  `show_results` bit(1) DEFAULT NULL,
  `shuffle_answers` bit(1) DEFAULT NULL,
  `shuffle_questions` bit(1) DEFAULT NULL,
  `start_time` datetime(6) DEFAULT NULL,
  `time_limit_minutes` int DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `type` enum('PRACTICE','ASSESSMENT') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(255) NOT NULL,
  `lesson_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_hbtoywagqvo25l2h4eswmxsov` (`lesson_id`),
  KEY `FKievk66v823e0do1n39ts4p0e7` (`created_by`),
  CONSTRAINT `FKi10d7n4lf738sm3mon21aubik` FOREIGN KEY (`lesson_id`) REFERENCES `lesson` (`id`),
  CONSTRAINT `FKievk66v823e0do1n39ts4p0e7` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `quiz_answer`
--

DROP TABLE IF EXISTS `quiz_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_answer` (
  `id` varchar(255) NOT NULL,
  `answer_text` tinytext NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_correct` bit(1) DEFAULT NULL,
  `order_index` int DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `question_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK4miyop5d48hmevxd77k797gsp` (`question_id`),
  CONSTRAINT `FK4miyop5d48hmevxd77k797gsp` FOREIGN KEY (`question_id`) REFERENCES `quiz_question` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `quiz_attempt`
--

DROP TABLE IF EXISTS `quiz_attempt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_attempt` (
  `id` varchar(255) NOT NULL,
  `attempt_number` int DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `correct_answers` int DEFAULT NULL,
  `incorrect_answers` int DEFAULT NULL,
  `is_passed` bit(1) DEFAULT NULL,
  `percentage` double DEFAULT NULL,
  `score` double DEFAULT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `status` enum('IN_PROGRESS','COMPLETED','ABANDONED','EXPIRED') DEFAULT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `total_questions` int DEFAULT NULL,
  `unanswered_questions` int DEFAULT NULL,
  `quiz_id` varchar(255) NOT NULL,
  `student_id` varchar(255) NOT NULL,
  `enrollment_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8l6wmgul0rgeha0lp6abrp5fa` (`quiz_id`),
  KEY `FK194kea5wb63isv082v8wthsug` (`student_id`),
  KEY `FK1mfvh50tb8kir4uvilp6btaaw` (`enrollment_id`),
  CONSTRAINT `FK194kea5wb63isv082v8wthsug` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FK1mfvh50tb8kir4uvilp6btaaw` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollment` (`id`),
  CONSTRAINT `FK8l6wmgul0rgeha0lp6abrp5fa` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `quiz_attempt_answer`
--

DROP TABLE IF EXISTS `quiz_attempt_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_attempt_answer` (
  `id` varchar(255) NOT NULL,
  `answered_at` datetime(6) DEFAULT NULL,
  `is_correct` bit(1) DEFAULT NULL,
  `points_earned` double DEFAULT NULL,
  `attempt_id` varchar(255) NOT NULL,
  `question_id` varchar(255) NOT NULL,
  `selected_answer_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKq3nrkyywe5ou7bsoqdevy1jt9` (`attempt_id`),
  KEY `FK2k5wh70eushis179y4h8htut1` (`question_id`),
  KEY `FK5giowl6kfgl1a7a2koutc6s8q` (`selected_answer_id`),
  CONSTRAINT `FK2k5wh70eushis179y4h8htut1` FOREIGN KEY (`question_id`) REFERENCES `quiz_question` (`id`),
  CONSTRAINT `FK5giowl6kfgl1a7a2koutc6s8q` FOREIGN KEY (`selected_answer_id`) REFERENCES `quiz_answer` (`id`),
  CONSTRAINT `FKq3nrkyywe5ou7bsoqdevy1jt9` FOREIGN KEY (`attempt_id`) REFERENCES `quiz_attempt` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `quiz_question`
--

DROP TABLE IF EXISTS `quiz_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_question` (
  `id` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `explanation` tinytext,
  `order_index` int DEFAULT NULL,
  `points` double DEFAULT NULL,
  `question_text` tinytext NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `quiz_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_quiz_order_index` (`quiz_id`,`order_index`),
  CONSTRAINT `FKdtynvfjgh6e7fd8l0wk37nrpc` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `name` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role_permissions`
--

DROP TABLE IF EXISTS `role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permissions` (
  `role_name` varchar(255) NOT NULL,
  `permissions_name` varchar(255) NOT NULL,
  PRIMARY KEY (`role_name`,`permissions_name`),
  KEY `FKf5aljih4mxtdgalvr7xvngfn1` (`permissions_name`),
  CONSTRAINT `FKcppvu8fk24eqqn6q4hws7ajux` FOREIGN KEY (`role_name`) REFERENCES `role` (`name`),
  CONSTRAINT `FKf5aljih4mxtdgalvr7xvngfn1` FOREIGN KEY (`permissions_name`) REFERENCES `permission` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `uploaded_file`
--

DROP TABLE IF EXISTS `uploaded_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `uploaded_file` (
  `id` varchar(255) NOT NULL,
  `content_type` varchar(255) DEFAULT NULL,
  `file_name` varchar(255) NOT NULL,
  `is_public` bit(1) NOT NULL,
  `original_file_name` varchar(255) DEFAULT NULL,
  `uploaded_at` datetime(6) DEFAULT NULL,
  `course_id` varchar(255) DEFAULT NULL,
  `uploaded_by_id` varchar(255) DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_3gaoms3rxeq6gfff1dvbqmqa3` (`file_name`),
  KEY `FK24ta90jdbdeo55q7x1yoqg8oa` (`course_id`),
  KEY `FK3el27hs9cqfrxucqhcuesh7rx` (`uploaded_by_id`),
  CONSTRAINT `FK24ta90jdbdeo55q7x1yoqg8oa` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`),
  CONSTRAINT `FK3el27hs9cqfrxucqhcuesh7rx` FOREIGN KEY (`uploaded_by_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` varchar(255) NOT NULL,
  `dob` date DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `bio` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `gender` enum('MALE','FEMALE','OTHER') DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `login_fail_count` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` varchar(255) NOT NULL,
  `roles_name` varchar(255) NOT NULL,
  PRIMARY KEY (`user_id`,`roles_name`),
  KEY `FK6pmbiap985ue1c0qjic44pxlc` (`roles_name`),
  CONSTRAINT `FK55itppkw3i07do3h7qoclqd4k` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FK6pmbiap985ue1c0qjic44pxlc` FOREIGN KEY (`roles_name`) REFERENCES `role` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'identity'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-06-11 22:34:36
