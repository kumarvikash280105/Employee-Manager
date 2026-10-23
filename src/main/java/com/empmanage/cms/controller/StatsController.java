package com.empmanage.cms.controller;

import com.empmanage.cms.dto.ApiResponse;
import com.empmanage.cms.dto.DashboardStatsDto;
import com.empmanage.cms.entity.Department;
import com.empmanage.cms.repository.DepartmentRepository;
import com.empmanage.cms.repository.EmployeeRepository;
import com.empmanage.cms.repository.UserRepository;
import com.empmanage.cms.service.AttendanceService;
import com.empmanage.cms.service.ContactService;
import com.empmanage.cms.service.DepartmentService;
import com.empmanage.cms.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private ContactService contactService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getStats() {
        DashboardStatsDto stats = new DashboardStatsDto();

        stats.setRegisteredUsers(userRepository.count());
        stats.setTotalEmployees(employeeService.getTotalCount());
        stats.setActiveEmployees(employeeService.getActiveCount());
        stats.setTotalDepartments(departmentRepository.count());
        stats.setPresentEmployeesToday(attendanceService.getTodayPresentCount());
        stats.setAbsentEmployeesToday(attendanceService.getTodayAbsentCount());
        stats.setAttendanceRate(attendanceService.getTodayAttendanceRate());
        stats.setPendingInquiries(contactService.getPendingCount());

        // Department-wise employee breakdown (Requirement 5)
        List<Map<String, Object>> deptBreakdown = new ArrayList<>();
        List<Department> departments = departmentService.getAllDepartments();
        for (Department d : departments) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("name", d.getName());
            map.put("code", d.getCode());
            map.put("count", employeeRepository.countByDepartment_Id(d.getId()));
            deptBreakdown.add(map);
        }
        stats.setDepartmentBreakdown(deptBreakdown);

        // Recent 5 employees
        stats.setRecentEmployees(employeeService.getRecentEmployees());

        // Weekly attendance trends for charts
        stats.setAttendanceTrends(attendanceService.getAttendanceTrends());

        return ResponseEntity.ok(ApiResponse.ok("Live dashboard statistics.", stats));
    }
}
