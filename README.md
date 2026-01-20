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

### Planned Features
- Leave approval/rejection (HR)
- Email notifications
- Yearly report generation (PDF/Text)
- Employee self-profile update

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

## Notes

- Ensure `serviceAccountKey.json` is in the project root directory
- RMI Server must be running before starting the client
- Default RMI port: 1099
