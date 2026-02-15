# Distributed Computer System Assignment
# RMI Employee Management System

A distributed employee management system built with Java RMI and Firebase.

## Project Structure

```
src/main/java/
├── database/
│   └── AuthService.java        # Business logic (Firebase operations)
├── server/
│   ├── AuthInterface.java      # 1. Remote Interface
│   ├── AuthServiceImpl.java    # 2. Remote Object Implementation
│   ├── RMIServer.java          # 3. RMI Server (start this first)
│   └── RMIClient.java          # 4. RMI Client (main entry point)
├── hr/
│   └── HRMenu.java             # HR Dashboard menu
└── employee/
    └── EmployeeMenu.java       # Employee Dashboard menu
```

## RMI Architecture

```
┌─────────────────┐         ┌─────────────────────────────────────┐
│   RMI Client    │         │            RMI Server               │
│  (RMIClient)    │         │                                     │
├─────────────────┤  RMI    ├─────────────────────────────────────┤
│                 │ ------> │  AuthInterface (Remote Interface)   │
│  - Login        │ Port    │              ↓                      │
│  - HR Menu      │ 1099    │  AuthServiceImpl (Remote Object)    │
│  - Employee Menu│         │              ↓                      │
│                 │ <------ │  AuthService (Firebase Operations)  │
└─────────────────┘         └─────────────────────────────────────┘
```

## How to Run

### 1. Start the RMI Server
```bash
# Compile
mvn clean compile

# Run server
mvn exec:java -Dexec.mainClass="server.RMIServer"
```

### 2. Start the RMI Client (in a separate terminal)
```bash
mvn exec:java -Dexec.mainClass="server.RMIClient"
```

### Or run from IDE:
1. Run `server.RMIServer` first
2. Run `server.RMIClient` second

## Features

### Current Features
- **Login System** - Firebase Authentication with role-based access control
- **HR Functions:**
  - View all employees
  - Add new employee (auto-creates leave balance)
  - Edit employee details
  - Delete employee
  - Manage payroll (CRUD)
  - View pending leave requests
  - Approve/reject leave applications (with balance validation)
  - Generate yearly reports (downloadable as text files)
- **Employee Functions:**
  - View profile
  - Update profile (email and personal information)
  - View payroll history
  - Apply for leave (annual, emergency, medical)
  - Check leave status and balance
  - View leave history

### Planned Features
- Email notifications for leave approvals/rejections
- PDF report generation (currently text-based)
- Advanced analytics and reporting

---

## Development Team & Tasks

### Dev 1 - System Architect & Admin
| Task | Status |
|------|--------|
| Create Database Schema (Users) | ✅ Done |
| Create Database Schema (Leaves) | ✅ Done |
| Server Setup: RMI Registry & Connection | ✅ Done |
| HR Task: Register Employee | ✅ Done |
| HR Task: View Employee List | ✅ Done |

### Dev 2 - Employee Functionality
| Task | Status |
|------|--------|
| Employee Task: Apply for Leave | ✅ Done |
| Employee Task: Check Leave Status | ✅ Done |
| Employee Task: View Salary | ✅ Done |
| UI: Employee Dashboard Main Menu | ✅ Done |

### Dev 3 - Profile & Notifications
| Task | Status |
|------|--------|
| Employee Task: Fetch Profile | ✅ Done |
| Employee Task: Update Profile | ✅ Done |
| System Task: Email Logic | ❌ Pending |
| Security: Password Hashing | ✅ Done (Firebase) |

### Dev 4 - HR Operations
| Task | Status |
|------|--------|
| HR Task: View Pending Leave Requests | ✅ Done |
| HR Task: Approve/Reject Leave | ✅ Done |
| HR Task: Generate Yearly Report | ✅ Done |
| UI: HR Dashboard Main Menu | ✅ Done |

---

## Database Schema (Firebase Firestore)

### Users Collection
```
/users/{uid}
├── email: string
├── first_name: string
├── last_name: string
├── ic_passport: string
└── role: string ("hr" | "employee")
```

### Payroll_Salary Collection
```
/Payroll_Salary/{payroll_id}
├── payroll_id: string
├── userid: string
├── Salary: number
├── Month_Entry: string ("01" - "12")
└── Year_Entry: string ("2024")
```

### Leaves Collection
```
/Leaves/{leave_id}
├── leave_id: string
├── userid: string
├── leave_type: string ("annual" | "emergency" | "medical")
├── start_date: string
├── end_date: string
├── total_days: number
├── reason: string
├── status: string ("pending" | "approved" | "rejected")
└── date_created_at: timestamp
```

### Leave_Balance Collection
```
/Leave_Balance/{leave_balance_id}
├── leave_balance_id: string
├── userid: string
├── year: string
├── annual_leave: number
├── emergency_leave: number
└── medical_leave: number
```

---

## Technology Stack

- **Language:** Java 21
- **RMI:** Java Remote Method Invocation
- **Database:** Firebase Firestore
- **Authentication:** Firebase Auth
- **Build Tool:** Maven

## Dependencies (pom.xml)

- Firebase Admin SDK
- Google Auth Library
- Gson (JSON parsing)

---

## Test Accounts

| Role | Email | Password |
|------|-------|----------|
| HR | (your HR email) | (password) |
| Employee | (your employee email) | (password) |

---

## Configuration Files

### serviceAccountKey.json

This file contains Firebase service account credentials for server-side authentication. **Never commit this file to Git!**

**Fields explained:**
| Field | Description |
|-------|-------------|
| `type` | Always "service_account" for Firebase |
| `project_id` | Your Firebase project ID |
| `private_key_id` | Unique identifier for the private key |
| `private_key` | RSA private key for authentication (keep secret!) |
| `client_email` | Service account email address |
| `client_id` | Unique client identifier |
| `auth_uri` | Google OAuth2 authentication endpoint |
| `token_uri` | Google OAuth2 token endpoint |
| `auth_provider_x509_cert_url` | Google's public certificate URL |
| `client_x509_cert_url` | Service account's public certificate URL |

**How to obtain this file:**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click the gear icon > **Project Settings**
4. Go to **Service Accounts** tab
5. Click **Generate new private key**
6. Save the downloaded file as `serviceAccountKey.json` in the project root

**Security Warning:** This file grants full access to your Firebase project. Never share it or commit it to version control.

---

## Notes

- Ensure `serviceAccountKey.json` is in the project root directory
- RMI Server must be running before starting the client
- Default RMI port: 1099
- Leave balance is automatically created when new employees are registered
- HR can download yearly reports to a specified file path
