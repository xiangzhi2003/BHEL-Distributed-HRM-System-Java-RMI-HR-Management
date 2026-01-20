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
- **Login System** - Firebase Authentication
- **HR Functions:**
  - View all employees
  - Add new employee
  - Edit employee details
  - Delete employee
  - Manage payroll (CRUD)
- **Employee Functions:**
  - View profile
  - View payroll history
  - Leave application system (database ready)

### Planned Features
- Leave approval/rejection (HR)
- Email notifications
- Yearly report generation (PDF/Text)
- Employee self-profile update

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
| Employee Task: Apply for Leave | ❌ Pending |
| Employee Task: Check Leave Status | ❌ Pending |
| Employee Task: View Salary | ✅ Done |
| UI: Employee Dashboard Main Menu | ⚠️ Partial |

### Dev 3 - Profile & Notifications
| Task | Status |
|------|--------|
| Employee Task: Fetch Profile | ✅ Done |
| Employee Task: Update Profile | ❌ Pending |
| System Task: Email Logic | ❌ Pending |
| Security: Password Hashing | ✅ Done (Firebase) |

### Dev 4 - HR Operations
| Task | Status |
|------|--------|
| HR Task: View Pending Leave Requests | ❌ Pending |
| HR Task: Approve/Reject Leave | ❌ Pending |
| HR Task: Generate Yearly Report | ❌ Pending |
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

- **Language:** Java 11+
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

## Notes

- Ensure `serviceAccountKey.json` is in the project root directory
- RMI Server must be running before starting the client
- Default RMI port: 1099
