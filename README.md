# 🏢 EmpManage – Enterprise CMS, Workforce & Role Permission Platform

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.3-green.svg?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18%2B-blue.svg?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Tests](https://img.shields.io/badge/Tests-33%2F33%20Passing-success.svg?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/kumarvikash280105/Employee-Manager)
[![Port](https://img.shields.io/badge/Server%20Port-8090-blueviolet.svg)](#-how-to-run--deploy)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](#)

> **EmpManage** is an enterprise-grade full-stack workforce and dynamic CMS management system designed with clean **Controller–Service–Repository** design patterns, strict **Spring Security 6 method authorization (`@PreAuthorize`)**, a resilient **PostgreSQL** persistence layer, and an interactive **Role & Granular Permission Matrix Studio**.

---

## 📑 Table of Contents

1. [🌟 System Overview & Key Highlights](#-system-overview--key-highlights)
2. [✨ Core Enterprise Modules](#-core-enterprise-modules)
   - [🛡️ Role & Permission Matrix Studio](#️-role--permission-matrix-studio)
   - [👥 Workforce Management (Employees)](#-workforce-management-employees)
   - [🏢 Organizational Departments](#-organizational-departments)
   - [🕒 Shift & Attendance Punch Clock](#-shift--attendance-punch-clock)
   - [📊 Operational Dashboard & Analytics](#-operational-dashboard--analytics)
   - [📝 Dynamic Website CMS Hub](#-dynamic-website-cms-hub)
   - [📥 Safe Reports & Inquiries](#-safe-reports--inquiries)
3. [🛠️ Technology Stack](#️-technology-stack)
4. [📂 Project Directory Structure](#-project-directory-structure)
5. [🏛️ Architecture & Security Hardening](#️-architecture--security-hardening)
6. [🗄️ Database Schema & Relational Model](#️-database-schema--relational-model)
7. [🔑 Complete 22-Permission Matrix Reference](#-complete-22-permission-matrix-reference)
8. [📡 REST API Endpoints Specification](#-rest-api-endpoints-specification)
9. [🧪 Automated Test Suite (33/33 Tests Passing)](#-automated-test-suite-3333-tests-passing)
10. [🔑 Pre-Configured Demo Credentials](#-pre-configured-demo-credentials)
11. [💻 Terminal Se Run Karne Ka Sara Format (Complete Terminal Execution Guide)](#-terminal-se-run-karne-ka-sara-format-complete-terminal-execution-guide)
    - [⚡ Quick Cheatsheet (1 Command Run)](#-quick-cheatsheet-1-command-run)
    - [📋 Prerequisites Verification](#-prerequisites-verification)
    - [🖥️ Format 1: Windows PowerShell / VS Code Terminal](#️-format-1-windows-powershell--vs-code-terminal)
    - [💻 Format 2: Windows Command Prompt (CMD)](#-format-2-windows-command-prompt-cmd)
    - [🚀 Format 3: Direct Maven Plugin (`mvn spring-boot:run`)](#-format-3-direct-maven-plugin-mvn-spring-bootrun)
    - [⚡ Format 4: Batch File Launcher (`start-server.bat`)](#-format-4-batch-file-launcher-start-serverbat)
    - [⚙️ Format 5: Custom Port ya Custom Database Credentials](#️-format-5-custom-port-ya-custom-database-credentials)
    - [🧪 Format 6: Terminal se Automated Tests Run Karna](#-format-6-terminal-se-automated-tests-run-karna)
    - [🛑 Server Stop / Kill Commands](#-server-stop--kill-commands)
    - [🌐 Access URLs Summary](#-access-urls-summary)

---

## 🌟 System Overview & Key Highlights

- **Complete RBAC & Granular Permission Governance:** 7 core modules, 22 permissions, custom roles, dynamic user role assignments, and session refresh.
- **Enterprise UI Design:** Clean, responsive, glassmorphic dashboards using zero-dependency modern vanilla JavaScript (ES6+), semantic HTML5, and CSS variables.
- **Spring Security 6 Architecture:** Route and method-level access authorization (`@EnableMethodSecurity`, `hasAuthority()`, `hasRole()`), BCrypt hashing (10 salt rounds), and stateful session tracking.
- **PostgreSQL 18 Persistence:** Fully relational database structure with JPA cascade controls, foreign keys, unique constraints, and safe deletion guards.
- **Dedicated Port `8090`:** Eliminates conflicts with default `8080` (Tomcat) or `8085` background services.

---

## ✨ Core Enterprise Modules

### 🛡️ Role & Permission Matrix Studio (`roles-permissions.html`)
- **Permission Matrix Table View (Default):**
  - Displays all **7 Core Modules** as rows and **5 Operations** (`👁️ View`, `➕ Create`, `✏️ Edit`, `🗑️ Delete`, `📥 Export`) as columns.
  - Interactive status cells with real-time checkboxes (`✓ Granted` / `Allow`) and soft green highlighting.
  - Non-applicable actions render a clean `—` dash.
- **Column-Level 1-Click Toggles:**
  - Header `Toggle` buttons grant or revoke an entire permission type (e.g., all `VIEW` or all `EXPORT` permissions) across every module in one click.
- **Row-Level Quick Actions:**
  - `All` / `Clear` buttons per module row with real-time status counter badges (e.g., `5/5`, `2/2`).
- **Dual Table Switcher:**
  - Switch instantly between **📊 Matrix Table** and **📋 Detailed Table** (full row breakdown displaying permission keys, technical codes, and descriptions).
- **Safety & Protection Guards:**
  - Permanent master administrator protection (`ROLE_ADMIN` cannot be deleted or deactivated).
  - Last administrator demotion guard.
  - Role deletion guard (roles with active user assignments cannot be deleted).
- **Security Audit Trail:**
  - Comprehensive immutable logging in `role_audit_logs` capturing user modifications, timestamp, performed-by identity, and JSON diffs.

### 👥 Workforce Management (Employees)
- Full CRUD operations with server-side validation (`@NotBlank`, `@Email`, `@Pattern`).
- Automatic employee code generation (`EMP-001`, `EMP-002`, etc.).
- Department assignment via JPA `@ManyToOne` relationship.
- Multi-parameter search by name, email, employee code, or designation with pagination support.
- Duplicate email collision checks for both new onboarding and record updates.

### 🏢 Organizational Departments
- True `@OneToMany` association with workforce employees.
- Real-time headcount calculation (`countByDepartment_Id`).
- **Deletion Guard:** Blocks department deletion with `409 Conflict` if any employees are currently assigned to that department.

### 🕒 Shift & Attendance Punch Clock
- One-click **Clock In / Clock Out** punch toggle with real-time working shift duration calculations.
- Identity derived strictly from the authenticated backend session (`SecurityContextHolder`), preventing identity spoofing.
- Employee view: Personalized attendance log history.
- Administrator view: Complete company-wide attendance timesheets with status override capability (`Present`, `WFH`, `On Leave`, `Absent`).

### 📊 Operational Dashboard & Analytics
- Live PostgreSQL-computed KPI metrics: Registered Users, Workforce Count, Active Departments, Present Today, Absent/On-Leave Today, and Attendance Percentages.
- Visual department breakdown bars and 7-day attendance trend distribution.

### 📝 Dynamic Website CMS Hub
- Real-time management of public-facing website copy: **Hero Banner**, **About Us**, **Core Features**, and **Contact Info**.
- Whitelist key validation (`hero`, `about`, `contact`, `features`, `footer`).
- Built-in regex **XSS sanitization pipeline** stripping script tags, event handlers (`onerror=`, `onload=`), and dangerous schemes (`javascript:`).

### 📥 Safe Reports & Inquiries
- Instant CSV export for Workforce Directories and Attendance Timesheets.
- **CSV Formula Injection Defense:** Automatically neutralizes spreadsheet calculation injection vectors (`=`, `+`, `-`, `@`, `\t`, `\r`) with single-quote escaping.
- UTF-8 BOM encoding for seamless opening in Microsoft Excel, Apple Numbers, and Google Sheets.
- Public contact message inbox with administrative status tracking (`PENDING` / `RESOLVED`).

---

## 🛠️ Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend Framework** | Java 21 LTS, Spring Boot 3.3.5 |
| **Security & Auth** | Spring Security 6.3, BCrypt (Cost Factor 10), Stateful Session Auth, Method Security (`@PreAuthorize`) |
| **Database & ORM** | PostgreSQL 18+, Spring Data JPA, Hibernate ORM 6.5, HikariCP Connection Pool |
| **Frontend UI** | Modern Vanilla JavaScript (ES6+), Semantic HTML5, CSS Custom Properties (Zero Heavy External Frameworks) |
| **Build & Tooling** | Apache Maven 3.8+, Maven Surefire Plugin, Spring Boot Maven Plugin |
| **Testing Suite** | JUnit 5 (Jupiter), Mockito 5.11, AssertJ |
| **Data Formats** | RESTful JSON, RFC 4180 Compliant Secure CSV |

---

## 📂 Project Directory Structure

```text
c:\Users\Vikash Kumar\OneDrive\Desktop\New Project\
├── pom.xml                                    # Maven Build Configuration
├── README.md                                  # Complete Project Documentation
├── start-server.bat                           # 1-Click Windows Server Launcher
├── index.html                                 # Public Website & Employee Shift Clock
├── admin.html                                 # Master Operational Management Cockpit
├── roles-permissions.html                     # Enterprise Role & Permission Studio
├── css/
│   └── style.css                              # Core Design System & Responsive Styles
├── js/
│   ├── app.js                                 # Public Portal & Clock In/Out Logic
│   ├── admin.js                               # Admin Operations & Dashboard Logic
│   └── roles-permissions.js                   # Permission Matrix Table & Studio Logic
├── src/
│   ├── main/
│   │   ├── java/com/empmanage/cms/
│   │   │   ├── EmpManageCmsApplication.java   # Spring Boot Application Entry Point
│   │   │   ├── config/                        # SecurityConfig, WebConfig, AuditConfig
│   │   │   ├── controller/                    # REST Controllers (Auth, Role, Employee, etc.)
│   │   │   ├── dto/                           # Data Transfer Objects & Validation Models
│   │   │   ├── entity/                        # JPA Relational Entities (10 Tables)
│   │   │   ├── init/                          # DataInitializer (Seed Data & RBAC Defaults)
│   │   │   ├── repository/                    # Spring Data JPA Repositories
│   │   │   └── service/                       # Business Logic & Transactional Services
│   │   └── resources/
│   │       ├── application.properties         # Server & Datasource Configurations
│   │       └── static/                        # Packaged Frontend Assets
│   └── test/
│       └── java/com/empmanage/cms/service/    # 33 JUnit 5 & Mockito Unit Tests
└── target/
    └── empmanage-cms-1.0.0.jar                # Production Standalone Executable Archive
```

---

## 🏛️ Architecture & Security Hardening

```text
 ┌───────────────────────────────────────────────────────────────────┐
 │                  Client Tier (Web Browser)                        │
 │   - Public Portal & Punch Clock (index.html)                      │
 │   - Administrative Operations Cockpit (admin.html)                │
 │   - Role & Permission Matrix Studio (roles-permissions.html)      │
 └─────────────────────────────────┬─────────────────────────────────┘
                                   │ HTTP / JSON REST (Port 8090)
 ┌─────────────────────────────────▼─────────────────────────────────┐
 │               Spring Boot 3.3.5 Application Tier                  │
 │                                                                   │
 │  [Spring Security 6 Filter Chain]                                 │
 │   ├── BCrypt Authentication Provider (10 Salt Rounds)             │
 │   ├── Role & Permission Authority Mapper (GrantedAuthority)       │
 │   └── Method Security Interceptor (@PreAuthorize)                 │
 │                                                                   │
 │  [REST Controllers]                                               │
 │   ├── AuthController & RoleController                             │
 │   ├── EmployeeController & DepartmentController                   │
 │   └── AttendanceController, CmsController & ReportController      │
 │                                                                   │
 │  [Transactional Service Layer]                                    │
 │   ├── Safe Deletion Guards & Validation Pipelines                 │
 │   ├── XSS Sanitization & CSV Formula Injection Shield             │
 │   └── Immutable Security Audit Logging Service                    │
 └─────────────────────────────────┬─────────────────────────────────┘
                                   │ JDBC Connection (HikariCP)
 ┌─────────────────────────────────▼─────────────────────────────────┐
 │               PostgreSQL Relational Database                      │
 │               empmanage_cms_db (Port 5432)                        │
 │   - 10 Relational Tables with Foreign Keys & Unique Constraints   │
 └───────────────────────────────────────────────────────────────────┘
```

### Key Security Safeguards:
1. **Zero Plaintext Credentials:** All passwords strictly encoded via `BCryptPasswordEncoder(10)`.
2. **Deny-by-Default Authorization:** Endpoints require explicit role or permission authorities; unauthenticated requests receive `401 Unauthorized` or `403 Forbidden`.
3. **Impersonation Defense:** Attendance punch actions resolve the user from `SecurityContextHolder`, never accepting user ID from client payloads.
4. **Session Synchronization:** Modifying user roles updates active sessions immediately, avoiding stale privilege elevation.
5. **CSV Formula Injection Shield:** Prepends `'` to cells beginning with `=`, `+`, `-`, `@`, `\t`, or `\r`.
6. **XSS Sanitizer:** Strips malicious script tags and inline event attributes from all incoming CMS mutations.

---

## 🗄️ Database Schema & Relational Model

The system utilizes **10 PostgreSQL relational tables**:

```text
 ┌──────────────┐          ┌───────────────────────┐
 │ departments  │ 1      * │       employees       │
 │  id (PK)     ├──────────┤  id (PK)              │
 │  name, code  │          │  department_id (FK)   │
 └──────────────┘          └───────────────────────┘

 ┌──────────────┐ 1      * ┌───────────────────────┐
 │    roles     ├──────────┤         users         │
 │  id (PK)     │          │  id (PK)              │
 │  name        │          │  role_id (FK)         │
 └──────┬───────┘          └───────────────────────┘
        │ *
        │                  ┌───────────────────────┐
        │  *               │  attendance_records   │
 ┌──────┴───────────────┐  │  id (PK)              │
 │   role_permissions   │  │  user_email, date_key │
 │  role_id (FK)        │  └───────────────────────┘
 │  permission_id (FK)  │
 └──────┬───────────────┘  ┌───────────────────────┐
        │ *                │     cms_sections      │
        │                  │  id (PK), section_key │
 ┌──────┴───────┐ 1        └───────────────────────┘
 │ permissions  │
 │  id (PK)     │          ┌───────────────────────┐
 │  name        │          │   contact_inquiries   │
 └──────────────┘          │  id (PK), status      │
                           └───────────────────────┘
 ┌──────────────────────┐
 │   role_audit_logs    │
 │  id (PK), action     │
 └──────────────────────┘
```

1. **`users`**: User credentials, BCrypt hashes, official email, department, and assigned role foreign key.
2. **`roles`**: System roles (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_HR_MANAGER`, etc.) and custom roles with active state flags.
3. **`permissions`**: 22 granular permission tokens classified by module and action.
4. **`role_permissions`**: Many-to-Many mapping table associating roles to permitted capabilities.
5. **`role_audit_logs`**: Immutable audit logs of all role assignments, permission edits, and policy changes.
6. **`departments`**: Organizational units with unique names, codes, heads, icons, and dynamic member counts.
7. **`employees`**: Workforce roster records with unique employee codes, emails, phone numbers, and department links.
8. **`attendance_records`**: Punch logs tracking punch-in/out timestamps, daily hours, work mode, and statuses.
9. **`cms_sections`**: Editable website sections (`hero`, `about`, `contact`, `features`, `footer`) with sanitized JSON content.
10. **`contact_inquiries`**: Public messages submitted through the website contact form with resolution workflows.

---

## 🔑 Complete 22-Permission Matrix Reference

The system divides authorization into **7 Core Modules** and **5 Primary Operations**:

| Module | Operation | Permission Key | Description |
| :--- | :---: | :--- | :--- |
| **👥 Workforce Employees** | `VIEW` | `EMPLOYEE_VIEW` | Browse employee roster and profiles |
| | `CREATE` | `EMPLOYEE_CREATE` | Onboard new personnel and add employee records |
| | `EDIT` | `EMPLOYEE_EDIT` | Update employee designations, departments, contact info |
| | `DELETE` | `EMPLOYEE_DELETE` | Remove workforce records |
| | `EXPORT` | `EMPLOYEE_EXPORT` | Export employee directory to CSV |
| **🏢 Organizational Departments** | `VIEW` | `DEPARTMENT_VIEW` | Inspect company departments and member counts |
| | `CREATE` | `DEPARTMENT_CREATE` | Establish new company business units |
| | `EDIT` | `DEPARTMENT_EDIT` | Update department names, heads, and descriptions |
| | `DELETE` | `DEPARTMENT_DELETE` | Delete empty departments (guarded against active members) |
| | `EXPORT` | `DEPARTMENT_EXPORT` | Export departmental statistics to CSV |
| **🕒 Time & Attendance Shifts** | `VIEW` | `ATTENDANCE_VIEW` | View company-wide punch clock timesheets |
| | `CREATE` | `ATTENDANCE_CREATE` | Self-service shift clock in / clock out |
| | `EDIT` | `ATTENDANCE_EDIT` | Administrative punch-out override and shift status changes |
| | `DELETE` | `ATTENDANCE_DELETE` | Remove invalid or corrupted attendance records |
| | `EXPORT` | `ATTENDANCE_EXPORT` | Export attendance timesheets to CSV |
| **📊 Reports & Analytics** | `VIEW` | `REPORT_VIEW` | View operational charts, shift ratios, workforce KPIs |
| | `EXPORT` | `REPORT_EXPORT` | Download executive analytical spreadsheets |
| **📝 Public Website CMS** | `VIEW` | `CMS_VIEW` | Preview dynamic website content and announcements |
| | `EDIT` | `CMS_EDIT` | Edit hero titles, about copy, features, contact details |
| **👤 User Accounts & Access** | `VIEW` | `USER_VIEW` | Inspect registered logins, emails, and account states |
| | `EDIT` | `USER_MANAGE` | Reset passwords, activate/deactivate accounts |
| **🛡️ Role Governance & RBAC** | `EDIT` | `ROLE_MANAGE` | Create custom roles and configure permission matrix |

---

## 📡 REST API Endpoints Specification

### 1. Authentication & User Management (`/api/auth`)
| Method | Endpoint | Required Authority | Description |
| :---: | :--- | :---: | :--- |
| `POST` | `/api/auth/register` | Public | Register standard employee account |
| `POST` | `/api/auth/login` | Public | Authenticate with email/alias & password |
| `POST` | `/api/auth/logout` | Authenticated | Terminate session & clear security context |
| `GET` | `/api/auth/me` | Authenticated | Return currently active session user profile |
| `GET` | `/api/auth/users` | `USER_VIEW` / Admin | Fetch all registered user accounts |
| `DELETE` | `/api/auth/users/{email}`| `USER_MANAGE` / Admin | Delete user account (protected against master admin) |

### 2. Role & Permission Management (`/api/roles`, `/api/permissions`)
| Method | Endpoint | Required Authority | Description |
| :---: | :--- | :---: | :--- |
| `GET` | `/api/roles` | `ROLE_MANAGE` / Admin | List all roles with assigned user counts |
| `GET` | `/api/roles/stats` | `ROLE_MANAGE` / Admin | Fetch role summary KPI metrics |
| `GET` | `/api/roles/{id}` | `ROLE_MANAGE` / Admin | Get role details by ID |
| `POST` | `/api/roles` | `ROLE_MANAGE` / Admin | Create custom role with permissions |
| `PUT` | `/api/roles/{id}` | `ROLE_MANAGE` / Admin | Update role metadata (name, description) |
| `DELETE` | `/api/roles/{id}` | `ROLE_MANAGE` / Admin | Delete role (guarded against assigned users & system roles) |
| `GET` | `/api/roles/{id}/permissions` | `ROLE_MANAGE` / Admin | Get granted permissions for role |
| `PUT` | `/api/roles/{id}/permissions` | `ROLE_MANAGE` / Admin | Save and synchronize permission matrix |
| `PUT` | `/api/roles/{id}/status` | `ROLE_MANAGE` / Admin | Toggle active/inactive status |
| `POST` | `/api/roles/assign-user` | `USER_MANAGE` / Admin | Assign role policy to user with session sync |
| `GET` | `/api/roles/audit-logs` | `ROLE_MANAGE` / Admin | Retrieve immutable security audit trail |
| `GET` | `/api/permissions` | `ROLE_MANAGE` / Admin | List all 22 system permissions |
| `GET` | `/api/permissions/matrix` | `ROLE_MANAGE` / Admin | Get permissions grouped by module |

### 3. Employees (`/api/employees`)
| Method | Endpoint | Required Authority | Description |
| :---: | :--- | :---: | :--- |
| `GET` | `/api/employees` | `EMPLOYEE_VIEW` | Search and filter paginated employees |
| `GET` | `/api/employees/{id}` | `EMPLOYEE_VIEW` | Fetch single employee details |
| `POST` | `/api/employees` | `EMPLOYEE_CREATE` | Create employee with department link |
| `PUT` | `/api/employees/{id}` | `EMPLOYEE_EDIT` | Update employee information |
| `DELETE` | `/api/employees/{id}` | `EMPLOYEE_DELETE` | Remove employee record |

### 4. Departments (`/api/departments`)
| Method | Endpoint | Required Authority | Description |
| :---: | :--- | :---: | :--- |
| `GET` | `/api/departments` | `DEPARTMENT_VIEW` | List all departments with dynamic headcounts |
| `GET` | `/api/departments/{id}` | `DEPARTMENT_VIEW` | Fetch department details |
| `POST` | `/api/departments` | `DEPARTMENT_CREATE` | Create organizational department |
| `PUT` | `/api/departments/{id}` | `DEPARTMENT_EDIT` | Update department details |
| `DELETE` | `/api/departments/{id}` | `DEPARTMENT_DELETE` | Delete empty department (guarded against active members) |

### 5. Attendance & Shifts (`/api/attendance`)
| Method | Endpoint | Required Authority | Description |
| :---: | :--- | :---: | :--- |
| `POST` | `/api/attendance/punch` | `ATTENDANCE_CREATE` | Clock In / Clock Out personal shift |
| `GET` | `/api/attendance/my` | Authenticated | Fetch personalized attendance logs |
| `GET` | `/api/attendance/all` | `ATTENDANCE_VIEW` | View company-wide attendance logs |
| `PUT` | `/api/attendance/{id}/status` | `ATTENDANCE_EDIT` | Override shift status (`Present`, `WFH`, `On Leave`) |
| `DELETE` | `/api/attendance/{id}` | `ATTENDANCE_DELETE` | Delete attendance record |

### 6. Dynamic CMS & Analytics (`/api/cms`, `/api/stats`, `/api/reports`)
| Method | Endpoint | Required Authority | Description |
| :---: | :--- | :---: | :--- |
| `GET` | `/api/cms/sections` | `CMS_VIEW` / Public | Fetch all website sections |
| `GET` | `/api/cms/sections/{key}` | `CMS_VIEW` / Public | Fetch specific section by key |
| `PUT` | `/api/cms/sections/{key}` | `CMS_EDIT` | Update and sanitize section copy |
| `GET` | `/api/stats` | `REPORT_VIEW` / Admin | Fetch live dashboard metrics & trends |
| `GET` | `/api/reports/employees/csv` | `EMPLOYEE_EXPORT` | Download safe CSV of workforce directory |
| `GET` | `/api/reports/attendance/csv` | `ATTENDANCE_EXPORT`| Download safe CSV of attendance timesheets |

---

## 🧪 Automated Test Suite (33/33 Tests Passing)

The project includes **33 comprehensive unit and integration tests** verifying critical business logic and security boundaries:

```bash
mvn test
```

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.empmanage.cms.service.AttendanceServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.empmanage.cms.service.CmsServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.empmanage.cms.service.DepartmentServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.empmanage.cms.service.EmployeeServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.empmanage.cms.service.RoleServiceTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.empmanage.cms.service.UserServiceTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 🔑 Pre-Configured Demo Credentials

The database automatically initializes with standard enterprise demo accounts:

| Role Policy | Email / Username | Password | Default Privileges |
| :--- | :--- | :--- | :--- |
| **👑 Master Administrator** | `admin@empmanage.com` *(or `admin`)* | `admin123` | Full unrestricted access to all 7 modules, CMS, Roles, and User Management |
| **💼 HR Manager** | `hr@empmanage.com` | `admin123` | Workforce CRUD, Departments CRUD, Shift Punch-Out, Reports, CMS Preview |
| **🏢 Department Manager** | `manager@empmanage.com` | `admin123` | Workforce View, Shift Logs, Department View, Personal Clock In/Out |
| **👤 Standard Employee** | `employee@empmanage.com` *(or `employee`)* | `emp123` | Personal Attendance Shift Clock, Public Directory View |
| **👁️ Read-Only Viewer** | `viewer@empmanage.com` | `admin123` | Read-only inspection across Workforce, Departments, Shifts, and Reports |

---

## 💻 Terminal Se Run Karne Ka Sara Format (Complete Terminal Execution Guide)

Ye section **Terminal (PowerShell, Command Prompt, Git Bash, VS Code Terminal)** se is project ko run karne ke sabhi formats, single-line commands, batch scripts aur troubleshooting steps provide karta hai.

---

### ⚡ Quick Cheatsheet (1 Command Run)

Agar project already build hai aur aap turant run karna chahte hain:
```bash
java -jar target/empmanage-cms-1.0.0.jar
```
Ya direct Maven se:
```bash
mvn spring-boot:run
```

---

### 📋 Prerequisites Verification

Server run karne se pehle terminal me ye 3 cheezein verify karein:

```powershell
# 1. Java 21 LTS installed hona chahiye
java -version

# 2. Apache Maven check karein
mvn -v

# 3. PostgreSQL Service status check karein (Windows PowerShell)
Get-Service *postgres*

# Agar PostgreSQL Service STOPPED hai toh start karein (PowerShell as Admin):
Start-Service postgresql-x64-18
# Ya CMD as Admin me:
net start postgresql-x64-18
```

> **Database Setup Note:** PostgreSQL me database ka naam `empmanage_cms_db` hona chahiye (Default User: `postgres`, Password: `admin`, Port: `5432`). Agar database create nahi hai toh psql ya pgAdmin me run karein:  
> `CREATE DATABASE empmanage_cms_db;`

---

### 🖥️ Format 1: Windows PowerShell / VS Code Terminal

#### Step 1: Project Directory me navigate karein
```powershell
cd "c:\Users\Vikash Kumar\OneDrive\Desktop\New Project"
```

#### Step 2: Fresh Build & Package karein
```powershell
mvn clean package -DskipTests
```

#### Step 3: Server Start karein
```powershell
java -jar target/empmanage-cms-1.0.0.jar
```

#### ⚡ One-Liner (Build & Run dono ek sath):
```powershell
mvn clean package -DskipTests; java -jar target/empmanage-cms-1.0.0.jar
```

---

### 💻 Format 2: Windows Command Prompt (CMD)

#### Step 1: Project folder me navigate karein
```cmd
cd /d "c:\Users\Vikash Kumar\OneDrive\Desktop\New Project"
```

#### Step 2: Clean & Package JAR
```cmd
mvn clean package -DskipTests
```

#### Step 3: Run JAR
```cmd
java -jar target\empmanage-cms-1.0.0.jar
```

#### ⚡ One-Liner CMD (Build & Run ek sath):
```cmd
mvn clean package -DskipTests && java -jar target\empmanage-cms-1.0.0.jar
```

---

### 🚀 Format 3: Direct Maven Plugin (`mvn spring-boot:run`)

Agar aap bina JAR file banaye direct live source code se run karna chahte hain (Development & Testing ke liye best):

```bash
mvn spring-boot:run
```

---

### ⚡ Format 4: Batch File Launcher (`start-server.bat`)

Project ke root folder me pre-configured batch launcher script maujood hai:
- **PowerShell me:**
  ```powershell
  .\start-server.bat
  ```
- **Command Prompt me:**
  ```cmd
  start-server.bat
  ```
- **Windows Explorer me:** Sirf `start-server.bat` file par **Double-Click** kar dein.

---

### ⚙️ Format 5: Custom Port ya Custom Database Credentials

Agar aap default port `8090` ya default database password change karke run karna chahte hain:

```bash
# Custom Port par run karne ke liye (e.g. Port 9090):
java -jar target/empmanage-cms-1.0.0.jar --server.port=9090

# Custom Database Password ke sath run karne ke liye:
java -jar target/empmanage-cms-1.0.0.jar --spring.datasource.password=YourNewPassword

# Port aur Database credentials dono customize karke run karna:
java -jar target/empmanage-cms-1.0.0.jar --server.port=8090 --spring.datasource.username=postgres --spring.datasource.password=admin
```

---

### 🧪 Format 6: Terminal se Automated Tests Run Karna

Backend ke sabhi 33 unit aur integration tests verify karne ke liye:

```bash
# Complete Test Suite run karein
mvn test

# Sirf Role & Permission Service tests run karein
mvn test -Dtest=RoleServiceTest

# Sirf User Authentication tests run karein
mvn test -Dtest=UserServiceTest
```

---

### 🛑 Server Stop / Kill Commands

1. **Normal Graceful Shutdown:** Terminal window me jahan server chal raha hai, keyboard se **`Ctrl + C`** press karein aur `Y` enter karein.
2. **Agar Port 8090 Busy / Occupied ho jaye (PowerShell):**
   ```powershell
   Get-NetTCPConnection -LocalPort 8090 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
   ```
3. **Agar Port 8090 Busy ho jaye (Command Prompt):**
   ```cmd
   netstat -ano | findstr :8090
   taskkill /PID <PID_NUMBER> /F
   ```

---

### 🌐 Access URLs Summary

Server start hone ke baad browser me in links ko open karein:

| Portal | URL | Description |
| :--- | :--- | :--- |
| **🌐 Public Homepage & Shift Clock** | [http://localhost:8090/](http://localhost:8090/) | Public landing page, Workforce Directory, Employee Shift Punch Clock |
| **⚙️ Admin Operations Cockpit** | [http://localhost:8090/admin.html](http://localhost:8090/admin.html) | Workforce CRUD, Attendance Overrides, Departments, CMS Editing |
| **🛡️ Role & Permission Matrix Studio** | [http://localhost:8090/roles-permissions.html](http://localhost:8090/roles-permissions.html) | Permission Matrix Table, 1-Click Column Toggles, User Role Assignment, Audit Trail |
