Deployed Link : [employee-leave-management-system-production.up.railway.app](employee-leave-management-system-production.up.railway.app)

Manager login : email    : abisheksankar15@gmail.com
                password : 123456789

# Employee Leave Management System

A robust, enterprise-grade Leave Management System built with **Spring Boot**, **Thymeleaf**, and **PostgreSQL**. This application streamlines the leave request process for both employees and managers with a modern, responsive interface.

## 🚀 Features

### For Employees
- **Leave Requests:** Apply for various leave types (Casual, Sick, Earned, etc.) with automated balance checks.
- **Dashboard:** View current leave balances and track the status of pending requests.
- **History:** Access a comprehensive history of all past leave applications.
- **Profile Management:** Update personal information and upload necessary documents.

### For Managers
- **Analytical Dashboard:** Real-time overview of team availability and pending approvals.
- **Leave Approval:** Seamlessly approve or reject leave requests with reason-tracking.
- **Employee Management:** Full CRUD operations for managing the employee database.
- **Policies:** Manage leave types and company-wide leave policies.

## 🛠️ Technology Stack

- **Backend:** Java 17, Spring Boot 3.x/4.x
- **Security:** Spring Security (Role-based access control)
- **Database:** PostgreSQL with Spring Data JPA & Hibernate
- **Frontend:** Thymeleaf, CSS, JavaScript (Nebula UI Theme)
- **Email:** Spring Boot Starter Mail (SMTP Integration)
- **Utilities:** Project Lombok, Maven, Spring Validation
- **File Handling:** Multi-part file upload for supporting documentation

## ⚙️ Configuration

1. **Database:** Update `src/main/resources/application.properties` with your PostgreSQL credentials.
2. **Email:** Configure your SMTP server details (e.g., Gmail) in `application.properties` for automated notifications.
3. **File Storage:** Ensure the `uploads/` directory has proper write permissions.

## 🏃 How to Run

1. Clone the repository:
   ```bash
   git clone https://github.com/ABISHEK-S-15/Employee-Leave-Management-System.git
   ```
2. Build the project:
   ```bash
   mvn clean install
   ```
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```
4. Access the portal at `http://localhost:8080`

## 📸 Screenshots

*(Add screenshots of your dashboards here)*

---
Developed by [ABISHEK-S-15](https://github.com/ABISHEK-S-15)
