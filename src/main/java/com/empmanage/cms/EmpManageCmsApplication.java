package com.empmanage.cms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EmpManageCmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmpManageCmsApplication.class, args);
        System.out.println("==========================================================");
        System.out.println("  EmpManage CMS & Spring Boot Backend Started Successfully");
        System.out.println("  Local URL:        http://localhost:8090/");
        System.out.println("  Admin Dashboard:  http://localhost:8090/admin.html");
        System.out.println("  Database:         PostgreSQL (empmanage_cms_db)");
        System.out.println("==========================================================");
    }
}
