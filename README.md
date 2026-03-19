# BHEL Distributed HRM System

A distributed Human Resource Management system built with Java RMI and Firebase.

## Architecture

```
┌─────────────┐         ┌──────────────────────────────────────┐
│  RMI Client │  RMI    │            RMI Server                │
│             │ ──────> │  AuthInterface (Remote Interface)    │
│  - Login    │ :1099   │           ↓                          │
│  - HR Menu  │         │  AuthServiceImpl (Remote Object)     │
│  - Employee │ <────── │           ↓                          │
│    Menu     │         │  AuthService → Firebase              │
└─────────────┘         └──────────────────────────────────────┘
```

## Project Structure

```
src/main/java/
├── database/
│   ├── AuthService.java        # Firebase business logic
│   └── EmailService.java       # Email notifications (Gmail SMTP)
├── server/
│   ├── AuthInterface.java      # RMI remote interface
│   ├── AuthServiceImpl.java    # RMI remote object
│   ├── RMIServer.java          # Server entry point
│   └── RMIClient.java          # Client entry point
├── hr/
│   └── HRMenu.java             # HR dashboard
└── employee/
    └── EmployeeMenu.java       # Employee dashboard
```

## How to Run

```bash
# 1. Start the server (terminal 1)
mvn clean compile
mvn exec:java -Dexec.mainClass="server.RMIServer"

# 2. Start the client (terminal 2)
mvn exec:java -Dexec.mainClass="server.RMIClient"
```

## Features

**HR**
- View / add / edit / delete employees
- Manage payroll (CRUD)
- View, approve and reject leave requests (with balance validation)
- Generate yearly leave reports

**Employee**
- View and update own profile
- View payroll history
- Apply for leave (annual / emergency / medical)
- View leave balance and history

**System**
- Role-based access control via Firebase Auth
- Automated email notifications via Gmail SMTP for all key actions
- NTP clock sync on startup
- SNMP health monitor (heap, uptime, threads every 30s)

## Setup

### 1. serviceAccountKey.json

Download from Firebase Console → Project Settings → Service Accounts → Generate new private key. Place it in the project root.

> Never commit this file — it is already in `.gitignore`.

### 2. Email Configuration

SMTP credentials (host, port, username, app password) are stored in Firebase Firestore under the `EmailConfig` collection. No hardcoded credentials in code.

To generate a Gmail App Password: Google Account → Security → 2-Step Verification → App Passwords.

## Tech Stack

| | |
|---|---|
| Language | Java 21 |
| Distribution | Java RMI |
| Database | Firebase Firestore |
| Authentication | Firebase Auth |
| Email | Gmail SMTP (JavaMail) |
| Build | Maven |

## Database Collections

| Collection | Purpose |
|---|---|
| `users` | Employee profiles and roles |
| `Payroll_Salary` | Monthly salary records |
| `Leave_Request` | Leave applications and status |
| `Leave_Balance` | Annual leave balance per employee |
| `EmailConfig` | SMTP configuration |
