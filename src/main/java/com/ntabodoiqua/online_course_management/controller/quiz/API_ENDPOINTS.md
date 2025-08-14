# Quiz API Endpoints Documentation

## Overview
Tầng controller cho chức năng Quiz đã được triển khai với 2 controller chính:
- **QuizController**: Quản lý quiz và câu hỏi
- **QuizAttemptController**: Quản lý việc làm bài quiz

## QuizController (`/quizzes`)

### CRUD Operations

#### 1. Tạo Quiz
```
POST /quizzes
Authorization: INSTRUCTOR, ADMIN
Content-Type: application/json

Body: QuizCreationRequest
{
    "title": "Java Basics Quiz",
    "description": "Test your Java knowledge",
    "type": "ASSESSMENT",
    "lessonId": "lesson-123",
    "passingScore": 75.0,
    "maxAttempts": 3,
    "timeLimitMinutes": 60,
    "questions": [
        {
            "questionText": "What is polymorphism?",
            "points": 5.0,
            "orderIndex": 1,
            "answers": [
                {
                    "answerText": "Ability of an object to take many forms",
                    "isCorrect": true,
                    "orderIndex": 1
                },
                {
                    "answerText": "A type of loop",
                    "isCorrect": false,
                    "orderIndex": 2
                }
            ]
        }
    ]
}

Response: QuizResponse
{
    "code": 1000,
    "message": "Quiz created successfully",
    "result": {
        "id": "quiz-uuid",
        "title": "Java Basics Quiz",
        "description": "Test your Java knowledge",
        "type": "ASSESSMENT",
        "lesson": {...},
        "createdBy": {...},
        "timeLimitMinutes": 60,
        "maxAttempts": 3,
        "passingScore": 75.0,
        "questions": null,  // Note: Questions not loaded in create response
        "totalQuestions": 0, // Use GET /quizzes/{id} to retrieve full data
        "isActive": true
    }
}
```

**⚠️ Lưu ý**: Response của POST không load questions để tối ưu hiệu năng. Sử dụng GET `/quizzes/{id}` để lấy quiz với đầy đủ questions.

#### 2. Lấy Quiz theo ID
```
GET /quizzes/{quizId}
Authorization: ALL (với phân quyền)

Response: QuizResponse (với đầy đủ questions và answers)
{
    "code": 1000,
    "message": "Quiz retrieved successfully",
    "result": {
        "id": "quiz-uuid",
        "title": "Java Basics Quiz",
        "description": "Test your Java knowledge",
        "type": "ASSESSMENT",
        "lesson": {...},
        "createdBy": {...},
        "questions": [
            {
                "id": "question-uuid",
                "questionText": "What is polymorphism?",
                "orderIndex": 1,
                "points": 5.0,
                "answers": [
                    {
                        "id": "answer-uuid",
                        "answerText": "Ability of an object to take many forms",
                        "orderIndex": 1,
                        "isCorrect": true
                    },
                    {
                        "id": "answer-uuid-2",
                        "answerText": "A type of loop",
                        "orderIndex": 2,
                        "isCorrect": false
                    }
                ]
            }
        ],
        "totalQuestions": 2,
        "isActive": true
    }
}
```

#### 3. Lấy Quiz cho Student
```
GET /quizzes/{quizId}/student
Authorization: STUDENT

Response: QuizStudentResponse (không có đáp án đúng)
```

#### 4. Cập nhật Quiz
```
PUT /quizzes/{quizId}
Authorization: INSTRUCTOR, ADMIN
Content-Type: application/json

Body: QuizUpdateRequest
{
    "title": "Updated Java Basics Quiz",
    "description": "Updated quiz to test Java knowledge",
    "type": "ASSESSMENT",
    "passingScore": 80.0,
    "maxAttempts": 2,
    "timeLimitMinutes": 45,
    "questions": [...]  // Questions trong request sẽ KHÔNG được update
}

Response: QuizResponse (với questions hiện tại, không phải từ request)
{
    "code": 1000,
    "message": "Quiz updated successfully",
    "result": {
        "id": "quiz-uuid",
        "title": "Updated Java Basics Quiz",  // ✅ Updated
        "description": "Updated quiz to test Java knowledge",  // ✅ Updated
        "passingScore": 80.0,  // ✅ Updated
        "maxAttempts": 2,  // ✅ Updated
        "timeLimitMinutes": 45,  // ✅ Updated
        "updatedAt": "2025-06-08T13:08:51.4305274",  // ✅ Updated
        "questions": [...],  // ❌ Existing questions, not from request
        "totalQuestions": 2  // ❌ Unchanged
    }
}
```

**⚠️ Lưu ý quan trọng**: 
- PUT `/quizzes/{id}` chỉ cập nhật **metadata** của quiz (title, description, settings)
- **Questions KHÔNG được cập nhật** qua endpoint này
- Để quản lý questions, sử dụng các endpoints riêng:
  - `POST /quizzes/{id}/questions` - Thêm question
  - `PUT /quizzes/questions/{questionId}` - Cập nhật question
  - `DELETE /quizzes/questions/{questionId}` - Xóa question

#### 5. Xóa Quiz
```
DELETE /quizzes/{quizId}
Authorization: INSTRUCTOR, ADMIN

Response: Success message
```

#### 6. Toggle Trạng thái Quiz
```
PATCH /quizzes/{quizId}/toggle-status
Authorization: INSTRUCTOR, ADMIN

Response: QuizResponse
```

### Quiz Questions Management

**⚠️ Important Note về OrderIndex:**
- `orderIndex` phải **UNIQUE** trong mỗi quiz
- Nếu orderIndex đã tồn tại, system nên:
  - **Option A**: Reject request với error message
  - **Option B**: Auto-increment orderIndex của question mới
  - **Option C**: Shift existing questions và insert vào vị trí mong muốn

#### 1. Thêm Câu hỏi
```
POST /quizzes/{quizId}/questions
Authorization: INSTRUCTOR, ADMIN
Content-Type: application/json

Body: QuizQuestionRequest
{
    "questionText": "What is inheritance in Java?",
    "points": 6.0,
    "orderIndex": 3,
    "answers": [
        {
            "answerText": "A mechanism where one class acquires properties of another",
            "isCorrect": true,
            "orderIndex": 1
        },
        {
            "answerText": "A type of loop",
            "isCorrect": false,
            "orderIndex": 2
        },
        {
            "answerText": "A method to sort arrays",
            "isCorrect": false,
            "orderIndex": 3
        }
    ]
}

Response: QuizQuestionResponse
{
    "code": 1000,
    "message": "Question added successfully",
    "result": {
        "id": "9a7df4be-8cf1-4996-a29a-f6def4406ca9",
        "questionText": "What is inheritance in Java?",
        "orderIndex": 3,
        "points": 6.0,
        "explanation": null,
        "createdAt": "2025-06-08T13:15:29.1674738",
        "updatedAt": "2025-06-08T13:15:29.1674738",
        "answers": [
            {
                "id": "aaca67fb-c901-4d65-9d3e-d6066b8084b9",
                "answerText": "A mechanism where one class acquires properties of another",
                "orderIndex": 1,
                "isCorrect": true,
                "createdAt": "2025-06-08T13:15:29.1765005",
                "updatedAt": "2025-06-08T13:15:29.1765005"
            },
            {
                "id": "d3ef11ba-6431-4ff7-b9d3-32edd5016d84",
                "answerText": "A type of loop",
                "orderIndex": 2,
                "isCorrect": false,
                "createdAt": "2025-06-08T13:15:29.1788629",
                "updatedAt": "2025-06-08T13:15:29.1788629"
            },
            {
                "id": "7c481d00-fce5-4901-8591-e170655a2b50",
                "answerText": "A method to sort arrays",
                "orderIndex": 3,
                "isCorrect": false,
                "createdAt": "2025-06-08T13:15:29.1788629",
                "updatedAt": "2025-06-08T13:15:29.1788629"
            }
        ]
    }
}
```

#### 2. Cập nhật Câu hỏi
```
PUT /quizzes/questions/{questionId}
Authorization: INSTRUCTOR, ADMIN
Content-Type: application/json

Body: QuizQuestionRequest
```

#### 3. Xóa Câu hỏi
```
DELETE /quizzes/questions/{questionId}
Authorization: INSTRUCTOR, ADMIN

Response: Success message
```

### Search & Filter

#### 1. Lấy Danh sách Quiz
```
GET /quizzes?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: ALL (với phân quyền)

Query Parameters:
- page: Trang hiện tại (default: 0)
- size: Kích thước trang (default: 10)
- sortBy: Sắp xếp theo (default: createdAt)
- sortDir: Hướng sắp xếp (default: desc)
- title: Tìm kiếm theo tiêu đề
- type: Lọc theo loại quiz (PRACTICE, ASSESSMENT)
- lessonId: Lọc theo lesson
- isActive: Lọc theo trạng thái

Response: Page<QuizResponse>
```

#### 2. Lấy Quiz theo Course
```
GET /quizzes/course/{courseId}
Authorization: ALL (với phân quyền theo role)

Response: List<QuizResponse>
{
    "code": 1000,
    "message": "Course quizzes retrieved successfully",
    "result": [
        {
            "id": "e4c2af2d-9fa4-49aa-996e-7e2f229d2a26",
            "title": "Updated Java Basics Quiz",
            "description": "Updated quiz to test Java knowledge",
            "type": "ASSESSMENT",
            "lesson": {
                "id": "d94faf74-006b-4eb7-a153-e62bde66cb4f",
                "title": "Update 3",
                "description": null,
                "content": "Update 3",
                "createdAt": "2025-06-06T16:37:27.903354",
                "updatedAt": "2025-06-07T17:32:22.093994",
                "createdBy": {
                    "id": "ea0c7864-91d9-420b-91d4-d22aeafa8dc1",
                    "username": "nguyenvan",
                    "firstName": "Van",
                    "lastName": "Nguyen",
                    "dob": "2000-08-10",
                    "avatarUrl": "/uploads/public/82b9b160-0c13-4620-bd43-7849d519d003_z6520715495476_cb9e415423c97b6a7cb4e08f5c20dfce.jpg",
                    "email": "anhnta2@gmail.com",
                    "phone": "0966277100",
                    "bio": "Hello",
                    "gender": "MALE",
                    "createdAt": "2025-05-28T09:49:22.333197",
                    "roles": [...],
                    "enabled": true
                },
                "courseCount": null
            },
            "createdBy": {
                "id": "ea0c7864-91d9-420b-91d4-d22aeafa8dc1",
                "username": "nguyenvan",
                "firstName": "Van",
                "lastName": "Nguyen",
                "dob": "2000-08-10",
                "avatarUrl": "/uploads/public/82b9b160-0c13-4620-bd43-7849d519d003_z6520715495476_cb9e415423c97b6a7cb4e08f5c20dfce.jpg",
                "email": "anhnta2@gmail.com",
                "phone": "0966277100",
                "bio": "Hello",
                "gender": "MALE",
                "createdAt": "2025-05-28T09:49:22.333197",
                "roles": [...],
                "enabled": true
            },
            "startTime": null,
            "endTime": null,
            "timeLimitMinutes": 45,
            "maxAttempts": 2,
            "passingScore": 80.0,
            "scoringMethod": "HIGHEST",
            "createdAt": "2025-06-08T12:58:30.706795",
            "updatedAt": "2025-06-08T13:13:18.143713",
            "questions": null,
            "totalQuestions": 4,
            "totalAttempts": 1,
            "isActive": true,
            "shuffleQuestions": null,
            "shuffleAnswers": null,
            "showResults": null,
            "showCorrectAnswers": null
        }
    ]
}
```

**✅ Key Features:**
- **Role-Based Access**: Endpoint available cho tất cả roles với appropriate filtering
- **Complete Course View**: All quizzes trong course (tùy theo permissions)
- **Rich Context**: Full lesson và creator information
- **Management Ready**: Suitable cho both student view và instructor management

**📊 Current Response Analysis:**
- **questions = null**: Indicates role-based filtering (possible student access hoặc security policy)
- **isActive = true**: Only active quizzes shown
- **totalAttempts = 1**: Shows actual usage statistics
- **Complete metadata**: Full quiz configuration visible

**🔄 Comparison với /available endpoint:**
- **Same structure**: Response format identical to /available
- **Same filtering**: questions = null in both cases
- **Same data richness**: Complete lesson và creator info
- **Same security**: No sensitive data exposure

**📋 Use Cases:**
- Course management dashboard
- Instructor quiz overview  
- Administrative course review
- Student course content discovery

#### 3. Lấy Quiz Available
```
GET /quizzes/course/{courseId}/available
Authorization: STUDENT

Response: List<QuizResponse> (chỉ quiz active và available)
{
    "code": 1000,
    "message": "Available quizzes retrieved successfully",
    "result": [
        {
            "id": "e4c2af2d-9fa4-49aa-996e-7e2f229d2a26",
            "title": "Updated Java Basics Quiz",
            "description": "Updated quiz to test Java knowledge",
            "type": "ASSESSMENT",
            "lesson": {
                "id": "d94faf74-006b-4eb7-a153-e62bde66cb4f",
                "title": "Update 3",
                "description": null,
                "content": "Update 3",
                "createdAt": "2025-06-06T16:37:27.903354",
                "updatedAt": "2025-06-07T17:32:22.093994",
                "createdBy": {
                    "id": "ea0c7864-91d9-420b-91d4-d22aeafa8dc1",
                    "username": "nguyenvan",
                    "firstName": "Van",
                    "lastName": "Nguyen",
                    "dob": "2000-08-10",
                    "avatarUrl": "/uploads/public/82b9b160-0c13-4620-bd43-7849d519d003_z6520715495476_cb9e415423c97b6a7cb4e08f5c20dfce.jpg",
                    "email": "anhnta2@gmail.com",
                    "phone": "0966277100",
                    "bio": "Hello",
                    "gender": "MALE",
                    "createdAt": "2025-05-28T09:49:22.333197",
                    "roles": [...],
                    "enabled": true
                },
                "courseCount": null
            },
            "createdBy": {
                "id": "ea0c7864-91d9-420b-91d4-d22aeafa8dc1",
                "username": "nguyenvan",
                "firstName": "Van",
                "lastName": "Nguyen",
                "dob": "2000-08-10",
                "avatarUrl": "/uploads/public/82b9b160-0c13-4620-bd43-7849d519d003_z6520715495476_cb9e415423c97b6a7cb4e08f5c20dfce.jpg",
                "email": "anhnta2@gmail.com",
                "phone": "0966277100",
                "bio": "Hello",
                "gender": "MALE",
                "createdAt": "2025-05-28T09:49:22.333197",
                "roles": [...],
                "enabled": true
            },
            "startTime": null,
            "endTime": null,
            "timeLimitMinutes": 45,
            "maxAttempts": 2,
            "passingScore": 80.0,
            "scoringMethod": "HIGHEST",
            "createdAt": "2025-06-08T12:58:30.706795",
            "updatedAt": "2025-06-08T13:13:18.143713",
            "questions": null,
            "totalQuestions": 4,
            "totalAttempts": 1,
            "isActive": true,
            "shuffleQuestions": null,
            "shuffleAnswers": null,
            "showResults": null,
            "showCorrectAnswers": null
        }
    ]
}
```

**✅ Key Features:**
- **Student-Safe Response**: questions = null (không expose quiz content)
- **Complete Metadata**: Full quiz configuration và settings
- **Lesson Context**: Complete lesson information với creator details
- **Attempt Tracking**: totalAttempts shows current usage (1 attempt recorded)
- **Instructor Information**: Full creator profile cho student reference

**📊 Available Quiz Criteria:**
- **isActive**: true (only active quizzes shown)
- **Student Access**: Student has access to course
- **No Time Restrictions**: startTime/endTime = null (always available)
- **Attempt Availability**: Students can still attempt (1/2 attempts used)

**🔒 Security Features:**
- **No Question Exposure**: questions field = null
- **No Answer Hints**: shuffleQuestions/shuffleAnswers = null
- **No Result Spoilers**: showResults/showCorrectAnswers = null
- **Basic Info Only**: Just enough info for student planning

**📋 Use Cases:**
- Course dashboard quiz listing
- Student learning path planning
- Quiz availability checking
- Assessment scheduling

#### 4. Lấy Thống kê Quiz
```
GET /quizzes/{quizId}/summary
Authorization: INSTRUCTOR, ADMIN

Response: QuizSummaryResponse
{
    "code": 1000,
    "message": "Quiz summary retrieved successfully",
    "result": {
        "id": "e4c2af2d-9fa4-49aa-996e-7e2f229d2a26",
        "title": "Updated Java Basics Quiz",
        "description": "Updated quiz to test Java knowledge",
        "type": "ASSESSMENT",
        "lessonId": "d94faf74-006b-4eb7-a153-e62bde66cb4f",
        "lessonTitle": "Update 3",
        "createdBy": {
            "id": "ea0c7864-91d9-420b-91d4-d22aeafa8dc1",
            "username": "nguyenvan",
            "firstName": "Van",
            "lastName": "Nguyen",
            "dob": "2000-08-10",
            "avatarUrl": "/uploads/public/82b9b160-0c13-4620-bd43-7849d519d003_z6520715495476_cb9e415423c97b6a7cb4e08f5c20dfce.jpg",
            "email": "anhnta2@gmail.com",
            "phone": "0966277100",
            "bio": "Hello",
            "gender": "MALE",
            "createdAt": "2025-05-28T09:49:22.333197",
            "roles": [
                {
                    "name": "STUDENT",
                    "description": "Student role",
                    "permissions": [
                        {
                            "name": "READ_DATA",
                            "description": "Read data permission"
                        }
                    ]
                },
                {
                    "name": "INSTRUCTOR",
                    "description": "Instructor role",
                    "permissions": [
                        {
                            "name": "READ_DATA",
                            "description": "Read data permission"
                        },
                        {
                            "name": "CREATE_DATA",
                            "description": "Create data permission"
                        },
                        {
                            "name": "UPDATE_DATA",
                            "description": "Update data permission"
                        }
                    ]
                }
                ],
            "enabled": true
        },
        "startTime": null,
        "endTime": null,
        "timeLimitMinutes": 45,
        "maxAttempts": 2,
        "passingScore": 80.0,
        "createdAt": "2025-06-08T12:58:30.706795",
        "updatedAt": "2025-06-08T13:13:18.143713",
        "totalQuestions": 4,
        "totalAttempts": 1,
        "passedAttempts": 0,
        "isActive": true
    }
}
```

**✅ Key Features:**
- **Complete Quiz Info**: Full quiz metadata và configuration
- **Creator Details**: Complete instructor information với roles và permissions
- **Statistics Summary**: Total attempts, passed attempts, questions count
- **Quiz Configuration**: Time limit, max attempts, passing score settings
- **Status Information**: Active status, creation/update timestamps

**📊 Current Statistics Analysis:**
- **Total Questions**: 4 questions configured
- **Total Attempts**: 1 student attempt completed  
- **Passed Attempts**: 0 (student scored 30.43% < 80% passing score)
- **Success Rate**: 0% (0/1 attempts passed)
- **Quiz Effectiveness**: May need review - current failure rate 100%

**👥 Creator Information:**
- **Instructor**: Van Nguyen (nguyenvan)
- **Dual Role**: Both STUDENT và INSTRUCTOR permissions
- **Contact**: anhnta2@gmail.com, 0966277100

**📋 Management Insights:**
- Quiz is active và available for students
- Single attempt recorded, student failed
- No time restrictions (startTime/endTime = null)  
- Students have 1 remaining attempt (maxAttempts = 2)

#### 9. Delete Question
```
DELETE /questions/{questionId}
// Alternative endpoint: DELETE /lms/quizzes/questions/{questionId}
Authorization: INSTRUCTOR, ADMIN (only creator or admin)

Success Response:
{
    "code": 1000,
    "message": "Question deleted successfully",
    "result": "Question with ID 66869f80-5bd3-442d-a33a-ecb9fb66b413 has been deleted"
}

Error Response - Question Has Attempts:
{
    "code": 1063,
    "message": "Question has attempts and cannot be deleted"
}
```

**🔒 Business Rule - Smart Data Integrity Protection:**
- **CAN Delete**: Questions WITHOUT existing attempts (fresh/unused questions)
- **CANNOT Delete**: Questions WITH existing attempts (historical data protection)
- **Logic**: Prevents data corruption while allowing cleanup of unused content
- **Impact**: Maintains quiz attempt history consistency

**✅ Tested Scenarios:**

**Scenario 1: Success - Unused Question**
- **Question ID**: `66869f80-5bd3-442d-a33a-ecb9fb66b413`
- **Status**: No attempts recorded
- **Result**: ✅ Deleted successfully (Code 1000)
- **Endpoint**: `/lms/quizzes/questions/{questionId}`

**Scenario 2: Blocked - Question with Attempts**
- **Question ID**: `e040df7d-146b-41ff-af4e-f6a33a6e4fde`
- **Status**: Has existing attempts
- **Result**: ❌ Deletion blocked (Code 1063)
- **Reason**: Data integrity protection

**🎯 Business Logic Analysis:**
- **Smart Deletion**: System allows deletion of unused questions only
- **Data Protection**: Historical attempts remain intact
- **Content Management**: Instructors can clean up draft/unused questions
- **Audit Trail**: Completed attempts preserve question references

**📊 Error Code Reference:**
- **1000**: Question deleted successfully (no attempts found)
- **1063**: Question has attempts and cannot be deleted (data protection)
- **Resolution**: For used questions, consider disable/archive functionality

## QuizAttemptController (`/quiz-attempts`)

### Quiz Attempt Management

#### 1. Bắt đầu Làm Quiz
```
POST /quiz-attempts/quiz/{quizId}/start
Authorization: STUDENT

Response: QuizAttemptResponse
{
    "code": 1000,
    "message": "Quiz attempt started successfully",
    "result": {
        "id": "e39a11f6-62cf-4efc-bb59-5427faaa760f",
        "quiz": {
            "id": "e4c2af2d-9fa4-49aa-996e-7e2f229d2a26",
            "title": "Updated Java Basics Quiz",
            "description": "Updated quiz to test Java knowledge",
            "type": "ASSESSMENT",
            "lesson": {
                "id": "d94faf74-006b-4eb7-a153-e62bde66cb4f",
                "title": "Update 3",
                "description": null,
                "content": "Update 3",
                "createdAt": "2025-06-06T16:37:27.903354",
                "updatedAt": "2025-06-07T17:32:22.093994",
                "createdBy": {...},
                "courseCount": null
            },
            "createdBy": {
                "id": "ea0c7864-91d9-420b-91d4-d22aeafa8dc1",
                "username": "nguyenvan",
                "firstName": "Van",
                "lastName": "Nguyen",
                "dob": "2000-08-10",
                "avatarUrl": "/uploads/public/82b9b160-0c13-4620-bd43-7849d519d003_z6520715495476_cb9e415423c97b6a7cb4e08f5c20dfce.jpg",
                "email": "anhnta2@gmail.com",
                "phone": "0966277100",
                "bio": "Hello",
                "gender": "MALE",
                "createdAt": "2025-05-28T09:49:22.333197",
                "roles": [...],
                "enabled": true
            },
            "startTime": null,
            "endTime": null,
            "timeLimitMinutes": 45,
            "maxAttempts": 2,
            "passingScore": 80.0,
            "scoringMethod": "HIGHEST",
            "createdAt": "2025-06-08T12:58:30.706795",
            "updatedAt": "2025-06-08T13:13:18.143713",
            "questions": null,
            "totalQuestions": 4,
            "totalAttempts": 0,
            "isActive": true,
            "shuffleQuestions": null,
            "shuffleAnswers": null,
            "showResults": null,
            "showCorrectAnswers": null
        },
        "student": {
            "id": "861e7675-7b3b-424e-a48e-c0fabe65e536",
            "username": "anhnta2004",
            "firstName": "Anh",
            "lastName": "Nguyen The",
            "dob": "2004-07-30",
            "avatarUrl": "/uploads/public/c9400dca-a5a6-4f3d-a82a-a74ee8bdc249_Marketing Icon 2.png",
            "email": "anhnta2004@gmail.com",
            "phone": "0966277109",
            "bio": "Top 1 at IT1-HUST",
            "gender": "MALE",
            "createdAt": "2025-05-28T00:28:32.023729",
            "roles": [
                {
                    "name": "STUDENT",
                    "description": "Student role",
                    "permissions": [
                        {
                            "name": "READ_DATA",
                            "description": "Read data permission"
                        }
                    ]
                }
            ],
            "enabled": true
        },
        "attemptNumber": 1,
        "startedAt": "2025-06-08T14:33:51.5814847",
        "submittedAt": null,
        "completedAt": null,
        "status": "IN_PROGRESS",
        "score": 0.0,
        "percentage": 0.0,
        "totalQuestions": 4,
        "correctAnswers": 0,
        "incorrectAnswers": 0,
        "unansweredQuestions": 4,
        "attemptAnswers": null,
        "isPassed": false
    }
}
```

**✅ Key Features:**
- **Student Information**: Đầy đủ thông tin student (đã được sửa lỗi mapping)
- **Quiz Details**: Complete quiz information với lesson và createdBy
- **Attempt Status**: Trạng thái ban đầu với score = 0, status = IN_PROGRESS
- **Time Tracking**: startedAt được ghi nhận, submittedAt và completedAt = null
- **Question Progress**: totalQuestions = 4, unansweredQuestions = 4

**📋 Business Logic:**
- API tự động tạo attempt với attemptNumber tăng dần
- Nếu có attempt IN_PROGRESS, trả về attempt đó (không tạo mới)
- Kiểm tra maxAttempts trước khi tạo attempt mới

#### 2. Lấy Attempt Hiện tại
```
GET /quiz-attempts/quiz/{quizId}/current
Authorization: STUDENT

Response: QuizAttemptResponse (nếu có attempt đang thực hiện)
{
    "code": 1000,
    "message": "Current attempt retrieved successfully",
    "result": {
        "id": "e39a11f6-62cf-4efc-bb59-5427faaa760f",
        "quiz": {
            "id": "e4c2af2d-9fa4-49aa-996e-7e2f229d2a26",
            "title": "Updated Java Basics Quiz",
            "description": "Updated quiz to test Java knowledge",
            "type": "ASSESSMENT",
            "lesson": {...},
            "createdBy": {...},
            "timeLimitMinutes": 45,
            "maxAttempts": 2,
            "passingScore": 80.0,
            "scoringMethod": "HIGHEST",
            "totalQuestions": 4,
            "totalAttempts": 1,
            "isActive": true
        },
        "student": {
            "id": "861e7675-7b3b-424e-a48e-c0fabe65e536",
            "username": "anhnta2004",
            "firstName": "Anh",
            "lastName": "Nguyen The",
            "dob": "2004-07-30",
            "avatarUrl": "/uploads/public/c9400dca-a5a6-4f3d-a82a-a74ee8bdc249_Marketing Icon 2.png",
            "email": "anhnta2004@gmail.com",
            "phone": "0966277109",
            "bio": "Top 1 at IT1-HUST",
            "gender": "MALE",
            "createdAt": "2025-05-28T00:28:32.023729",
            "roles": [...],
            "enabled": true
        },
        "attemptNumber": 1,
        "startedAt": "2025-06-08T14:33:51.581485",
        "submittedAt": null,
        "completedAt": null,
        "status": "IN_PROGRESS",
        "score": 0.0,
        "percentage": 0.0,
        "totalQuestions": 4,
        "correctAnswers": 0,
        "incorrectAnswers": 0,
        "unansweredQuestions": 4,
        "attemptAnswers": [
            {
                "id": "943cc7b8-fe1e-44ad-8700-c888d7579393",
                "question": {
                    "id": "31aba978-d9ee-44fd-a03d-0b3630be4f38",
                    "questionText": "What is the main method in Java?",
                    "orderIndex": 2,
                    "points": 5.0,
                    "explanation": null,
                    "answers": [
                        {
                            "id": "53942a46-59fc-474c-aaee-f38b13a6a744",
                            "answerText": "Entry point of a Java program",
                            "orderIndex": 1,
                            "isCorrect": true
                        },
                        {
                            "id": "5c8afb18-77eb-4318-8aa1-5e26502d46c2",
                            "answerText": "A constructor",
                            "orderIndex": 2,
                            "isCorrect": false
                        }
                    ]
                },
                "selectedAnswer": null,
                "pointsEarned": 0.0,
                "answeredAt": null,
                "isCorrect": false
            }
            // ... more attemptAnswers for remaining questions
        ],
        "isPassed": false
    }
}
```

**✅ Key Features:**
- **Complete Attempt Data**: Bao gồm tất cả questions với answers (sorted by orderIndex)
- **Student Information**: Đầy đủ thông tin student đã được map đúng
- **Progress Tracking**: attemptAnswers array cho phép tracking từng câu đã/chưa trả lời
- **Unanswered State**: selectedAnswer = null, pointsEarned = 0.0, answeredAt = null cho câu chưa trả lời

**⚠️ Security Note**: 
- API này trả về `isCorrect` field trong answers - có thể cần xem xét ẩn thông tin này
- Student có thể thấy đáp án đúng trước khi hoàn thành quiz

**📋 Use Cases:**
- Resume quiz từ điểm đã dừng
- Hiển thị progress (câu nào đã trả lời)
- Validate quiz state trước khi submit
- Auto-save functionality

#### 3. Trả lời Câu hỏi
```
POST /quiz-attempts/{attemptId}/questions/{questionId}/answer
Authorization: STUDENT
Content-Type: application/json

Body: QuizAttemptAnswerRequest
{
    "selectedAnswerId": "77ad9dce-da10-4562-aa4b-306dcfba264a"
}

Response: QuizAttemptAnswerResponse
{
    "code": 1000,
    "message": "Question answered successfully",
    "result": {
        "id": "425f6970-28db-45fe-8d65-e9a61167fc84",
        "question": {
            "id": "e040df7d-146b-41ff-af4e-f6a33a6e4fde",
            "questionText": "Updated: What is inheritance in Java?",
            "orderIndex": 4,
            "points": 7.0,
            "explanation": null,
            "createdAt": "2025-06-08T13:26:10.640157",
            "updatedAt": "2025-06-08T13:36:25.258106",
            "answers": [
                {
                    "id": "1dc2d3a2-fc23-4e04-bf26-71df057dacb1",
                    "answerText": "A type of exception handling",
                    "orderIndex": 2,
                    "createdAt": "2025-06-08T13:36:25.269994",
                    "updatedAt": "2025-06-08T13:36:25.269994",
                    "isCorrect": false
                },
                {
                    "id": "77ad9dce-da10-4562-aa4b-306dcfba264a",
                    "answerText": "Cập nhật đây",
                    "orderIndex": 1,
                    "createdAt": "2025-06-08T13:36:25.269009",
                    "updatedAt": "2025-06-08T13:36:25.269009",
                    "isCorrect": true
                }
            ]
        },
        "selectedAnswer": {
            "id": "77ad9dce-da10-4562-aa4b-306dcfba264a",
            "answerText": "Cập nhật đây",
            "orderIndex": 1,
            "createdAt": "2025-06-08T13:36:25.269009",
            "updatedAt": "2025-06-08T13:36:25.269009",
            "isCorrect": true
        },
        "pointsEarned": 7.0,
        "answeredAt": "2025-06-08T14:44:34.9547296",
        "isCorrect": true
    }
}
```

**✅ Key Features:**
- **Complete Question Data**: Full question object with all answers và metadata
- **Selected Answer Details**: Complete selected answer object với isCorrect status
- **Points Calculation**: pointsEarned = question.points nếu trả lời đúng, 0.0 nếu sai
- **Timestamp Tracking**: answeredAt được ghi nhận chính xác thời điểm trả lời
- **Answer Validation**: API validates selectedAnswerId thuộc về question đó

**📋 Business Logic:**
- Chỉ có thể trả lời khi attempt status = IN_PROGRESS
- Mỗi question chỉ có thể trả lời 1 lần (hoặc có thể update)
- Points được tính tự động dựa trên isCorrect
- Response bao gồm cả correct và incorrect answers (security concern noted)

#### 4. Nộp Bài Quiz
```
POST /quiz-attempts/{attemptId}/submit
Authorization: STUDENT

Response: QuizResultResponse
{
    "code": 1000,
    "message": "Quiz submitted successfully",
    "result": {
        "attemptId": "e39a11f6-62cf-4efc-bb59-5427faaa760f",
        "quizId": "e4c2af2d-9fa4-49aa-996e-7e2f229d2a26",
        "quizTitle": "Updated Java Basics Quiz",
        "attemptNumber": 1,
        "startedAt": "2025-06-08T14:33:51.581485",
        "completedAt": "2025-06-08T14:48:23.7854687",
        "durationMinutes": 14,
        "score": 7.0,
        "percentage": 30.434782608695656,
        "totalQuestions": 4,
        "correctAnswers": 0,
        "incorrectAnswers": 1,
        "unansweredQuestions": 3,
        "passingScore": 80.0,
        "feedback": "Keep studying and practicing. Don't give up!",
        "remainingAttempts": 1,
        "isPassed": false,
        "canRetake": true
    }
}
```

**✅ Key Features:**
- **Automatic Completion**: Status changed from IN_PROGRESS → COMPLETED
- **Score Calculation**: Total score từ tất cả questions đã trả lời
- **Percentage Precision**: High precision percentage calculation (30.434782608695656%)
- **Duration Tracking**: Exact duration in minutes (14 minutes)
- **Attempt Management**: remainingAttempts decreased từ 2 → 1
- **Result Assessment**: isPassed = false (score < passingScore)

**📊 Score Breakdown:**
- Total Possible Points: 23.0 (5+5+6+7 from all questions)
- Points Earned: 7.0 (from 1 correct answer)
- Percentage: 7.0/23.0 = 30.43%
- Result: FAILED (< 80% passing score)

**⚠️ Note về Scoring Logic:**
- `correctAnswers: 0` nhưng `score: 7.0` - có thể do logic tính correctAnswers khác với pointsEarned
- Student đã trả lời 1 câu và earn 7 points, nhưng correctAnswers counter = 0

**📋 Business Logic:**
- Quiz attempt status changed to COMPLETED
- Student có thể retake (canRetake: true, remainingAttempts: 1)
- Feedback message được generate based on performance level
- Duration calculated accurately từ start time đến completion time

### Student History & Statistics

#### 1. Lịch sử Làm bài
```
GET /quiz-attempts/quiz/{quizId}/history
Authorization: STUDENT

Response: List<QuizResultResponse>
{
    "code": 1000,
    "message": "Attempt history retrieved successfully",
    "result": [
        {
            "attemptId": "e39a11f6-62cf-4efc-bb59-5427faaa760f",
            "quizId": "e4c2af2d-9fa4-49aa-996e-7e2f229d2a26",
            "quizTitle": "Updated Java Basics Quiz",
            "attemptNumber": 1,
            "startedAt": "2025-06-08T14:33:51.581485",
            "completedAt": "2025-06-08T14:48:23.785469",
            "durationMinutes": 14,
            "score": 7.0,
            "percentage": 30.434782608695656,
            "totalQuestions": 4,
            "correctAnswers": 0,
            "incorrectAnswers": 1,
            "unansweredQuestions": 3,
            "passingScore": 80.0,
            "feedback": null,
            "remainingAttempts": null,
            "isPassed": false,
            "canRetake": null
        }
    ]
}
```

**✅ Key Features:**
- **Historical Records**: Complete attempt history cho specific quiz
- **Student Isolation**: Chỉ attempts của current student
- **Chronological Data**: Sorted by attemptNumber hoặc completedAt
- **Performance Tracking**: Đầy đủ score, percentage, duration cho mỗi attempt

**📊 History vs Current Attempt Differences:**
- `feedback`: null (history records không cần feedback)
- `remainingAttempts`: null (chỉ relevant cho current state)
- `canRetake`: null (chỉ relevant cho current state)
- All other fields: Identical to submit response

**📋 Use Cases:**
- Progress tracking qua multiple attempts
- Performance comparison between attempts
- Learning analytics và improvement measurement
- Decision support cho retake options

#### 2. Điểm Cao nhất
```
GET /quiz-attempts/quiz/{quizId}/best-score
Authorization: STUDENT

Response: QuizResultResponse
```

## Phân quyền (Authorization)

### ADMIN
- Toàn quyền CRUD với tất cả quiz
- Xem tất cả thống kê
- Không bị giới hạn bởi trạng thái active/inactive

### INSTRUCTOR
- CRUD quiz của mình
- Xem quiz active của instructor khác
- Xem thống kê quiz của mình
- Quản lý câu hỏi trong quiz của mình

### STUDENT
- Chỉ xem quiz active và có quyền truy cập
- Làm bài quiz theo quy định
- Xem kết quả và lịch sử của mình
- Không thể xem câu trả lời đúng (tùy cấu hình quiz)

## Error Handling

### Common Error Responses
```json
{
    "code": 1057,
    "message": "Quiz not found",
    "result": null
}
```

### Quiz Error Codes
- `1057`: QUIZ_NOT_FOUND
- `1058`: QUIZ_ALREADY_EXISTS_FOR_LESSON
- `1059`: QUIZ_TITLE_ALREADY_EXISTS
- `1060`: QUIZ_NOT_AVAILABLE
- `1061`: QUIZ_HAS_ATTEMPTS_CANNOT_DELETE
- `1062`: QUESTION_NOT_FOUND
- `1063`: QUESTION_HAS_ATTEMPTS_CANNOT_DELETE
- `1064`: QUESTION_MUST_HAVE_ANSWERS
- `1065`: QUESTION_MUST_HAVE_AT_LEAST_TWO_ANSWERS
- `1066`: QUESTION_MUST_HAVE_EXACTLY_ONE_CORRECT_ANSWER
- `1078`: QUESTION_ORDER_INDEX_ALREADY_EXISTS

### Quiz Attempt Error Codes
- `1067`: QUIZ_MAX_ATTEMPTS_EXCEEDED
- `1068`: QUIZ_ATTEMPT_NOT_FOUND
- `1069`: QUIZ_ATTEMPT_NOT_IN_PROGRESS
- `1070`: QUIZ_ATTEMPT_EXPIRED
- `1076`: QUIZ_NOT_STARTED
- `1077`: QUIZ_EXPIRED

## Usage Examples

### Frontend Integration Example

```javascript
// Tạo quiz và lấy thông tin đầy đủ
const createQuizAndGet = async (quizData) => {
    // Tạo quiz
    const createResponse = await fetch('/api/quizzes', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(quizData)
    });
    const createResult = await createResponse.json();
    
    // Lấy quiz với đầy đủ questions (vì POST response không có questions)
    if (createResult.code === 1000) {
        const getResponse = await fetch(`/api/quizzes/${createResult.result.id}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        return getResponse.json();
    }
    return createResult;
};

// Cập nhật quiz metadata (không bao gồm questions)
const updateQuizMetadata = async (quizId, updateData) => {
    const response = await fetch(`/api/quizzes/${quizId}`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            title: updateData.title,
            description: updateData.description,
            type: updateData.type,
            passingScore: updateData.passingScore,
            maxAttempts: updateData.maxAttempts,
            timeLimitMinutes: updateData.timeLimitMinutes
            // Không gửi questions - sẽ không được update
        })
    });
    return response.json();
};

// Quản lý questions riêng biệt
const addQuestionToQuiz = async (quizId, questionData) => {
    const response = await fetch(`/api/quizzes/${quizId}/questions`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(questionData)
    });
    return response.json();
};

const updateQuestion = async (questionId, questionData) => {
    const response = await fetch(`/api/quizzes/questions/${questionId}`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(questionData)
    });
    return response.json();
};

const deleteQuestion = async (questionId) => {
    const response = await fetch(`/api/quizzes/questions/${questionId}`, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });
    return response.json();
};

// Bắt đầu làm quiz
const startQuiz = async (quizId) => {
    const response = await fetch(`/api/quiz-attempts/quiz/${quizId}/start`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    });
    const result = await response.json();
    
    // Response structure: { code, message, result: { id, quiz, student, attemptNumber, ... } }
    if (result.code === 1000) {
        const attempt = result.result;
        console.log(`Quiz started: ${attempt.id}`);
        console.log(`Student: ${attempt.student.firstName} ${attempt.student.lastName}`);
        console.log(`Quiz: ${attempt.quiz.title} (${attempt.quiz.totalQuestions} questions)`);
        console.log(`Attempt: ${attempt.attemptNumber}/${attempt.quiz.maxAttempts}`);
    }
    
    return result;
};

// Lấy current attempt và resume quiz
const getCurrentAttempt = async (quizId) => {
    const response = await fetch(`/api/quiz-attempts/quiz/${quizId}/current`, {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });
    const result = await response.json();
    
    if (result.code === 1000 && result.result) {
        const attempt = result.result;
        console.log(`Resume attempt: ${attempt.id}`);
        console.log(`Progress: ${attempt.totalQuestions - attempt.unansweredQuestions}/${attempt.totalQuestions}`);
        
        // Process attemptAnswers to show current state
        attempt.attemptAnswers.forEach((attemptAnswer, index) => {
            const isAnswered = attemptAnswer.selectedAnswer !== null;
            console.log(`Q${index + 1}: ${attemptAnswer.question.questionText} - ${isAnswered ? 'Answered' : 'Unanswered'}`);
        });
        
        return attempt;
    }
    
    return null; // No current attempt
};

// Trả lời câu hỏi
const answerQuestion = async (attemptId, questionId, answerId) => {
    const response = await fetch(`/api/quiz-attempts/${attemptId}/questions/${questionId}/answer`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            selectedAnswerId: answerId
        })
    });
    const result = await response.json();
    
    if (result.code === 1000) {
        const answerData = result.result;
        console.log(`Question answered: ${answerData.question.questionText}`);
        console.log(`Selected: ${answerData.selectedAnswer.answerText}`);
        console.log(`Correct: ${answerData.isCorrect ? 'YES' : 'NO'}`);
        console.log(`Points earned: ${answerData.pointsEarned}/${answerData.question.points}`);
        console.log(`Answered at: ${answerData.answeredAt}`);
        
        // Update local quiz state
        return {
            questionId: answerData.question.id,
            selectedAnswer: answerData.selectedAnswer,
            isCorrect: answerData.isCorrect,
            pointsEarned: answerData.pointsEarned,
            answeredAt: answerData.answeredAt
        };
    }
    
    return result;
};

// Nộp bài
const submitQuiz = async (attemptId) => {
    const response = await fetch(`/api/quiz-attempts/${attemptId}/submit`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    });
    const result = await response.json();
    
    if (result.code === 1000) {
        const quizResult = result.result;
        console.log(`Quiz submitted: ${quizResult.quizTitle}`);
        console.log(`Score: ${quizResult.score} points (${quizResult.percentage.toFixed(2)}%)`);
        console.log(`Result: ${quizResult.isPassed ? 'PASSED' : 'FAILED'}`);
        console.log(`Duration: ${quizResult.durationMinutes} minutes`);
        console.log(`Feedback: ${quizResult.feedback}`);
        
        if (quizResult.canRetake) {
            console.log(`Can retake: ${quizResult.remainingAttempts} attempts remaining`);
        }
        
        // Return formatted result for UI
        return {
            success: true,
            attemptId: quizResult.attemptId,
            score: quizResult.score,
            percentage: quizResult.percentage,
            isPassed: quizResult.isPassed,
            feedback: quizResult.feedback,
            canRetake: quizResult.canRetake,
            remainingAttempts: quizResult.remainingAttempts,
            durationMinutes: quizResult.durationMinutes,
            completedAt: quizResult.completedAt
        };
    }
    
    return { success: false, error: result.message || 'Submit failed' };
};

// Lấy lịch sử attempts
const getAttemptHistory = async (quizId) => {
    const response = await fetch(`/api/quiz-attempts/quiz/${quizId}/history`, {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });
    const result = await response.json();
    
    if (result.code === 1000) {
        const attempts = result.result;
        console.log(`Found ${attempts.length} attempts for quiz`);
        
        // Display attempt summary
        attempts.forEach((attempt, index) => {
            console.log(`Attempt ${attempt.attemptNumber}:`);
            console.log(`  Score: ${attempt.score} (${attempt.percentage.toFixed(2)}%)`);
            console.log(`  Result: ${attempt.isPassed ? 'PASSED' : 'FAILED'}`);
            console.log(`  Duration: ${attempt.durationMinutes} minutes`);
            console.log(`  Date: ${new Date(attempt.completedAt).toLocaleDateString()}`);
        });
        
        // Calculate improvement
        if (attempts.length > 1) {
            const latest = attempts[attempts.length - 1];
            const first = attempts[0];
            const improvement = latest.percentage - first.percentage;
            console.log(`Overall improvement: ${improvement > 0 ? '+' : ''}${improvement.toFixed(2)}%`);
        }
        
        return {
            success: true,
            attempts: attempts,
            totalAttempts: attempts.length,
            bestScore: Math.max(...attempts.map(a => a.score)),
            bestPercentage: Math.max(...attempts.map(a => a.percentage)),
            averageScore: attempts.reduce((sum, a) => sum + a.score, 0) / attempts.length,
            averagePercentage: attempts.reduce((sum, a) => sum + a.percentage, 0) / attempts.length
        };
    }
    
    return { success: false, error: result.message || 'Failed to get history' };
};

// Lấy quiz summary (chỉ cho instructor/admin)
const getQuizSummary = async (quizId) => {
    const response = await fetch(`/api/quizzes/${quizId}/summary`, {
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    });
    const result = await response.json();
    
    if (result.code === 1000) {
        const summary = result.result;
        console.log(`Quiz Summary: ${summary.title}`);
        console.log(`Created by: ${summary.createdBy.firstName} ${summary.createdBy.lastName}`);
        console.log(`Type: ${summary.type} | Active: ${summary.isActive}`);
        console.log(`Questions: ${summary.totalQuestions} | Time Limit: ${summary.timeLimitMinutes} min`);
        console.log(`Max Attempts: ${summary.maxAttempts} | Passing Score: ${summary.passingScore}%`);
        
        // Statistics
        const successRate = summary.totalAttempts > 0 
            ? (summary.passedAttempts / summary.totalAttempts * 100).toFixed(2)
            : 0;
        console.log(`\nStatistics:`);
        console.log(`  Total Attempts: ${summary.totalAttempts}`);
        console.log(`  Passed: ${summary.passedAttempts}`);
        console.log(`  Success Rate: ${successRate}%`);
        
        // Recommendations
        if (summary.totalAttempts > 0 && summary.passedAttempts === 0) {
            console.log(`⚠️  Warning: No students have passed this quiz yet`);
            console.log(`💡 Consider reviewing quiz difficulty or questions`);
        }
        
        return {
            success: true,
            quiz: {
                id: summary.id,
                title: summary.title,
                type: summary.type,
                isActive: summary.isActive
            },
            statistics: {
                totalQuestions: summary.totalQuestions,
                totalAttempts: summary.totalAttempts,
                passedAttempts: summary.passedAttempts,
                successRate: parseFloat(successRate)
            },
            configuration: {
                timeLimitMinutes: summary.timeLimitMinutes,
                maxAttempts: summary.maxAttempts,
                passingScore: summary.passingScore
            },
            creator: {
                name: `${summary.createdBy.firstName} ${summary.createdBy.lastName}`,
                username: summary.createdBy.username,
                email: summary.createdBy.email
            }
        };
    }
    
    return { success: false, error: result.message || 'Failed to get summary' };
};

// Lấy available quizzes cho course (student view)
const getAvailableQuizzes = async (courseId) => {
    const response = await fetch(`/api/quizzes/course/${courseId}/available`, {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });
    const result = await response.json();
    
    if (result.code === 1000) {
        const quizzes = result.result;
        console.log(`Found ${quizzes.length} available quizzes in course`);
        
        // Process each quiz
        const processedQuizzes = quizzes.map(quiz => {
            const attemptsUsed = quiz.totalAttempts || 0;
            const attemptsRemaining = quiz.maxAttempts ? quiz.maxAttempts - attemptsUsed : 'Unlimited';
            const canAttempt = quiz.maxAttempts ? attemptsUsed < quiz.maxAttempts : true;
            
            console.log(`📝 ${quiz.title}`);
            console.log(`   Type: ${quiz.type} | Questions: ${quiz.totalQuestions}`);
            console.log(`   Time: ${quiz.timeLimitMinutes} min | Passing: ${quiz.passingScore}%`);
            console.log(`   Attempts: ${attemptsUsed}/${quiz.maxAttempts || '∞'} | Can attempt: ${canAttempt ? 'Yes' : 'No'}`);
            console.log(`   Lesson: ${quiz.lesson.title} by ${quiz.createdBy.firstName} ${quiz.createdBy.lastName}`);
            
            return {
                id: quiz.id,
                title: quiz.title,
                description: quiz.description,
                type: quiz.type,
                lessonTitle: quiz.lesson.title,
                instructorName: `${quiz.createdBy.firstName} ${quiz.createdBy.lastName}`,
                totalQuestions: quiz.totalQuestions,
                timeLimitMinutes: quiz.timeLimitMinutes,
                maxAttempts: quiz.maxAttempts,
                passingScore: quiz.passingScore,
                attemptsUsed: attemptsUsed,
                attemptsRemaining: attemptsRemaining,
                canAttempt: canAttempt,
                isActive: quiz.isActive
            };
        });
        
        // Filter by availability
        const availableQuizzes = processedQuizzes.filter(quiz => quiz.canAttempt && quiz.isActive);
        const completedQuizzes = processedQuizzes.filter(quiz => !quiz.canAttempt);
        
        console.log(`\n📊 Summary:`);
        console.log(`  Available to attempt: ${availableQuizzes.length}`);
        console.log(`  Attempts exhausted: ${completedQuizzes.length}`);
        
        return {
            success: true,
            allQuizzes: processedQuizzes,
            availableQuizzes: availableQuizzes,
            completedQuizzes: completedQuizzes,
            totalQuizzes: quizzes.length
        };
    }
    
    return { success: false, error: result.message || 'Failed to get available quizzes' };
};

// Lấy all quizzes trong course (role-based access)
const getCourseQuizzes = async (courseId) => {
    const response = await fetch(`/api/quizzes/course/${courseId}`, {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });
    const result = await response.json();
    
    if (result.code === 1000) {
        const quizzes = result.result;
        console.log(`Found ${quizzes.length} quizzes in course`);
        
        // Analyze quiz distribution
        const quizTypes = quizzes.reduce((acc, quiz) => {
            acc[quiz.type] = (acc[quiz.type] || 0) + 1;
            return acc;
        }, {});
        
        const activeQuizzes = quizzes.filter(quiz => quiz.isActive);
        const inactiveQuizzes = quizzes.filter(quiz => !quiz.isActive);
        
        console.log(`📊 Quiz Distribution:`);
        Object.entries(quizTypes).forEach(([type, count]) => {
            console.log(`  ${type}: ${count} quiz(s)`);
        });
        console.log(`📈 Status: ${activeQuizzes.length} active, ${inactiveQuizzes.length} inactive`);
        
        // Process quiz details
        const processedQuizzes = quizzes.map(quiz => {
            const difficultyLevel = quiz.passingScore >= 80 ? 'High' : 
                                  quiz.passingScore >= 60 ? 'Medium' : 'Low';
            
            console.log(`\n📝 ${quiz.title}`);
            console.log(`   Status: ${quiz.isActive ? 'Active' : 'Inactive'} | Type: ${quiz.type}`);
            console.log(`   Lesson: ${quiz.lesson.title}`);
            console.log(`   Difficulty: ${difficultyLevel} (${quiz.passingScore}% passing)`);
            console.log(`   Usage: ${quiz.totalAttempts} attempts | ${quiz.totalQuestions} questions`);
            console.log(`   Time: ${quiz.timeLimitMinutes} min | Max attempts: ${quiz.maxAttempts}`);
            
            return {
                id: quiz.id,
                title: quiz.title,
                type: quiz.type,
                isActive: quiz.isActive,
                lessonTitle: quiz.lesson.title,
                instructorName: `${quiz.createdBy.firstName} ${quiz.createdBy.lastName}`,
                difficulty: difficultyLevel,
                passingScore: quiz.passingScore,
                totalQuestions: quiz.totalQuestions,
                totalAttempts: quiz.totalAttempts,
                timeLimitMinutes: quiz.timeLimitMinutes,
                maxAttempts: quiz.maxAttempts,
                createdAt: quiz.createdAt,
                updatedAt: quiz.updatedAt
            };
        });
        
        return {
            success: true,
            quizzes: processedQuizzes,
            statistics: {
                total: quizzes.length,
                active: activeQuizzes.length,
                inactive: inactiveQuizzes.length,
                types: quizTypes
            },
            courseId: courseId
        };
    }
    
    return { success: false, error: result.message || 'Failed to get course quizzes' };
};

// Xóa question (với business rule protection)
const deleteQuestion = async (questionId) => {
    try {
        const response = await fetch(`/api/questions/${questionId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });
        
        const result = await response.json();
        
        if (result.code === 1000) {
            console.log(`✅ Question deleted successfully: ${questionId}`);
            console.log(`📝 Result: ${result.result}`);
            console.log(`🗑️ Status: Question was unused and safely removed`);
            
            return {
                success: true,
                message: result.message,
                questionId: questionId,
                deletionType: 'UNUSED_QUESTION'
            };
        }
        
        // Handle business rule violations
        if (result.code === 1063) {
            console.log(`🔒 Deletion blocked: ${result.message}`);
            console.log(`📊 Reason: Question has existing attempts - data integrity protected`);
            console.log(`💡 Suggestion: Consider disabling question instead of deletion`);
            
            return {
                success: false,
                error: result.message,
                errorCode: 1063,
                reason: 'BUSINESS_RULE_VIOLATION',
                suggestion: 'Use disable functionality instead of delete',
                questionId: questionId
            };
        }
        
        // Handle other errors
        console.log(`❌ Delete failed: ${result.message}`);
        return {
            success: false,
            error: result.message,
            errorCode: result.code,
            questionId: questionId
        };
        
    } catch (error) {
        console.error('❌ Network error during question deletion:', error);
        return {
            success: false,
            error: 'Network error occurred',
            networkError: true,
            questionId: questionId
        };
    }
};

// Usage example with error handling
const handleQuestionDeletion = async (questionId) => {
    console.log(`🗑️ Attempting to delete question: ${questionId}`);
    
    const result = await deleteQuestion(questionId);
    
    if (result.success) {
        // Update UI - remove question from list
        console.log(`🎉 Question removed from quiz`);
        // refreshQuestionsList();
    } else if (result.errorCode === 1063) {
        // Business rule violation - show user-friendly message
        alert(`Cannot delete question: ${result.error}\n\nSuggestion: ${result.suggestion}`);
        // showDisableQuestionOption(questionId);
    } else {
        // Other errors
        alert(`Failed to delete question: ${result.error}`);
    }
    
    return result;
};
```

## Testing

### Postman Test Collection

#### **Test Sequence Example:**
```javascript
// 1. Start Quiz
POST {{baseUrl}}/quiz-attempts/quiz/{{quizId}}/start

// 2. Answer Questions
POST {{baseUrl}}/quiz-attempts/{{attemptId}}/questions/{{questionId}}/answer
Body: { "selectedAnswerId": "{{answerId}}" }

// 3. Submit Quiz  
POST {{baseUrl}}/quiz-attempts/{{attemptId}}/submit

// 4. Get Attempt History
GET {{baseUrl}}/quiz-attempts/quiz/{{quizId}}/history

// 5. Get Best Score
GET {{baseUrl}}/quiz-attempts/quiz/{{quizId}}/best-score

// 6. Get Quiz Summary (Instructor/Admin only)
GET {{baseUrl}}/quizzes/{{quizId}}/summary

// 7. Get Available Quizzes in Course (Student)
GET {{baseUrl}}/quizzes/course/{{courseId}}/available

// 8. Get All Course Quizzes (Role-based)
GET {{baseUrl}}/quizzes/course/{{courseId}}

// 9. Delete Question (INSTRUCTOR/ADMIN only)
DELETE {{baseUrl}}/questions/{{questionId}}
// Alternative: DELETE {{baseUrl}}/lms/quizzes/questions/{{questionId}}

// Variables:
baseUrl = http://localhost:8080/lms
courseId = d6be5c9b-eb28-429b-a3fe-73fb05f99d42
quizId = e4c2af2d-9fa4-49aa-996e-7e2f229d2a26
attemptId = e39a11f6-62cf-4efc-bb59-5427faaa760f
questionId = e040df7d-146b-41ff-af4e-f6a33a6e4fde
answerId = 77ad9dce-da10-4562-aa4b-306dcfba264a
```

### Unit Test Example
```java
@Test
void testAnswerQuestion_Success() {
    QuizAttemptAnswerRequest request = QuizAttemptAnswerRequest.builder()
        .selectedAnswerId("77ad9dce-da10-4562-aa4b-306dcfba264a")
        .build();
    
    when(quizAttemptService.answerQuestion(attemptId, questionId, request))
        .thenReturn(mockAnswerResponse);
    
    ApiResponse<QuizAttemptAnswerResponse> response = 
        quizAttemptController.answerQuestion(attemptId, questionId, request);
    
    assertThat(response.getMessage()).isEqualTo("Question answered successfully");
    assertThat(response.getResult().getSelectedAnswer()).isNotNull();
    assertThat(response.getResult().getPointsEarned()).isEqualTo(7.0);
    assertThat(response.getResult().getIsCorrect()).isTrue();
}
```

## Performance Considerations

1. **Pagination**: Tất cả list endpoints đều hỗ trợ pagination
2. **Lazy Loading**: Chỉ load dữ liệu cần thiết
3. **Caching**: Có thể implement caching cho quiz data
4. **Database Indexing**: Đảm bảo có index cho các truy vấn thường xuyên
5. **Rate Limiting**: Có thể implement rate limiting cho quiz attempts

## Data Integrity Recommendations

### **OrderIndex Management:**
```sql
-- Đề xuất thêm unique constraint
ALTER TABLE quiz_questions 
ADD CONSTRAINT unique_quiz_order_index 
UNIQUE (quiz_id, order_index);

-- Index cho performance
CREATE INDEX idx_quiz_questions_order 
ON quiz_questions (quiz_id, order_index);
```

### **Business Logic:**
1. **Validate orderIndex** trước khi insert/update
2. **Auto-increment logic** khi conflict:
   ```java
   // Pseudo code
   if (orderIndexExists(quizId, orderIndex)) {
       // Option A: Reject
       throw new BusinessException("Order index already exists");
       
       // Option B: Auto-increment
       orderIndex = getNextAvailableOrderIndex(quizId);
       
       // Option C: Shift existing questions
       shiftQuestionsFromIndex(quizId, orderIndex);
   }
   ```
3. **Reorder API** để admin có thể sắp xếp lại:
   ```
       PUT /quizzes/{quizId}/questions/reorder
    Body: [
        {"questionId": "id1", "orderIndex": 1},
        {"questionId": "id2", "orderIndex": 2}
    ]
    ```

#### 4. Sắp xếp lại thứ tự câu hỏi
```
PUT /quizzes/{quizId}/questions/reorder
Authorization: INSTRUCTOR, ADMIN
Content-Type: application/json

Body: List<QuestionOrderRequest>
[
    {
        "questionId": "9a7df4be-8cf1-4996-a29a-f6def4406ca9",
        "orderIndex": 1
    },
    {
        "questionId": "31aba978-d9ee-44fd-a03d-0b3630be4f38", 
        "orderIndex": 2
    }
]

Response: List<QuizQuestionResponse>
{
    "code": 1000,
    "message": "Questions reordered successfully",
    "result": [
        {
            "id": "9a7df4be-8cf1-4996-a29a-f6def4406ca9",
            "questionText": "What is inheritance in Java?",
            "orderIndex": 1,
            "points": 6.0,
            "answers": [...]
        },
        {
            "id": "31aba978-d9ee-44fd-a03d-0b3630be4f38",
            "questionText": "What is the main method in Java?", 
            "orderIndex": 2,
            "points": 5.0,
            "answers": [...]
        }
    ]
}
```

## Security Features

1. **Authorization**: Tất cả endpoints đều có `@PreAuthorize`
2. **Input Validation**: Sử dụng `@Valid` cho request validation
3. **Ownership Check**: Kiểm tra quyền sở hữu trước khi modify
4. **Data Sanitization**: Validate input để tránh injection attacks
5. **Audit Logging**: Log các thao tác quan trọng

## Known Issues & Fixes

### **1. Student Mapping Issue in QuizAttemptResponse** ✅ **FIXED**
- **Problem**: `student` field was `null` in QuizAttemptResponse despite having student data in entity
- **Impact**: Frontend couldn't display student information, poor UX for tracking attempts
- **Root Cause**: Missing UserMapper dependency and student mapping in `QuizMapperFacade.toQuizAttemptResponseWithDetails()`
- **Solution**: 
  - Added `@Autowired private UserMapper userMapper;`
  - Added student mapping: `response.setStudent(userMapper.toUserResponse(attempt.getStudent()));`
- **API Response**: Now includes complete student information with roles and permissions

### **2. Duplicate OrderIndex Issue** ✅ **FIXED**
- **Problem**: Questions can have duplicate orderIndex within the same quiz
- **Impact**: Unpredictable question ordering, UX confusion  
- **Solution**: Added unique constraint validation + reorder API
- **Error Code**: `1078` - QUESTION_ORDER_INDEX_ALREADY_EXISTS

### **2. Duplicate Answers Issue** ✅ **FIXED**
- **Problem**: Question update creates duplicate answers instead of replacing
- **Root Cause**: Missing `@Modifying` and `@Transactional` on delete methods
- **Impact**: Multiple answers with same content, violates business rules (multiple correct answers)
- **Solution**: 
  - Added `@Modifying` and `@Transactional` to `deleteByQuestionId()`
  - Added `quizAnswerRepository.flush()` for immediate deletion
  - Improved answer orderIndex handling

### **3. Business Rule Validation** ✅ **ENHANCED**
- **Rule**: Each question must have exactly 1 correct answer
- **Validation**: Added in `validateQuestionAnswers()` method
- **Error Code**: `1066` - QUESTION_MUST_HAVE_EXACTLY_ONE_CORRECT_ANSWER

### **4. Security Issue: Answer Exposure in Current Attempt** ⚠️ **NEEDS REVIEW**
- **Problem**: GET `/quiz-attempts/quiz/{quizId}/current` exposes `isCorrect` field in answers
- **Impact**: Students can see correct answers before completing quiz, compromising assessment integrity
- **Risk Level**: HIGH for assessment quizzes, LOW for practice quizzes
- **Recommendation**: 
  - Use `QuizAnswerStudentResponse` (without isCorrect) for student-facing APIs
  - Only show correct answers after quiz completion or based on quiz configuration
  - Implement `showCorrectAnswers` quiz setting properly
- **Potential Fix**: Create separate response DTOs for in-progress vs completed attempts

### **5. Scoring Logic Inconsistency** ⚠️ **NEEDS INVESTIGATION**
- **Problem**: `correctAnswers: 0` nhưng `score: 7.0` trong submit response
- **Observed**: Student earned 7 points nhưng correctAnswers counter = 0
- **Impact**: Confusion trong reporting và analytics, incorrect progress tracking
- **Root Cause**: Có thể có inconsistency trong cách tính correctAnswers vs pointsEarned
- **Recommendation**: 
  - Review scoring calculation logic trong service layer
  - Ensure correctAnswers reflects actual số câu trả lời đúng
  - Synchronize pointsEarned với correctAnswers count
- **Expected**: correctAnswers = 1 (since score > 0 from correct answer)

### **6. History Response Design** ✅ **BY DESIGN**
- **Behavior**: History API returns `feedback`, `remainingAttempts`, `canRetake` as `null`
- **Rationale**: Historical records don't need current state information
- **Impact**: Frontend needs to handle null values appropriately
- **Design Decision**: 
  - History = pure historical data
  - Current attempt state = separate API calls
  - Reduces data redundancy và improves clarity

### **7. Identical Response: /course/{id} vs /course/{id}/available** ✅ **OBSERVED**
- **Behavior**: Both endpoints return identical response structures và data
- **Current State**: questions = null in both cases, same filtering applied
- **Possible Explanations**:
  - Security policy: questions never exposed in listing APIs
  - Role-based: Current user treated as student-level access
  - Business logic: Only individual quiz GET shows questions
- **Impact**: Frontend can use either endpoint interchangeably for current use case
- **Recommendation**: Clarify intended differences or consolidate endpoints

## Database Maintenance Scripts

### **Fix Duplicate OrderIndex:**
```sql
-- Execute: /src/main/resources/sql/quiz_orderindex_constraint.sql
```

### **Cleanup Duplicate Answers:**
```sql  
-- Execute: /src/main/resources/sql/cleanup_duplicate_answers.sql
``` 