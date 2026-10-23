document.addEventListener('DOMContentLoaded', async () => {

  const API_BASE = '/api';

  const getCurrentUser = () => {
    try {
      return JSON.parse(localStorage.getItem('empmanage_session_user') || 'null');
    } catch (e) {
      return null;
    }
  };

  const setCurrentUser = (u) => localStorage.setItem('empmanage_session_user', JSON.stringify(u));
  const removeCurrentUser = () => localStorage.removeItem('empmanage_session_user');

  let currentUser = getCurrentUser();

  const hasPermission = (permName) => {
    if (!currentUser) currentUser = getCurrentUser();
    if (!currentUser) return false;
    if (currentUser.permissions && Array.isArray(currentUser.permissions)) {
      return currentUser.permissions.includes(permName);
    }
    if (currentUser.roleEntity && currentUser.roleEntity.permissions) {
      return currentUser.roleEntity.permissions.some(p => (p.name || p) === permName);
    }
    return currentUser.role === 'ROLE_ADMIN' || currentUser.isAdmin === true;
  };

  // Validate session against server to prevent unauthorized client tampering (Security Requirement 1)
  const verifyAdminAccess = async () => {
    try {
      const res = await fetch(`${API_BASE}/auth/me`);
      if (res.ok) {
        const json = await res.json();
        const user = json.data;
        const isAdmin = user && (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN' || (user.role && user.role.toUpperCase().includes('ADMIN')) || user.displayRole === 'ADMIN' || (user.permissions && user.permissions.includes('ROLE_MANAGE')));
        if (isAdmin) {
          setCurrentUser(user);
          currentUser = user;
          const adminHeaderName = document.getElementById('adminHeaderName');
          if (adminHeaderName) adminHeaderName.textContent = currentUser.fullName;
          return true;
        }
      }
    } catch (e) {
      console.warn('Backend session verification failed:', e);
    }

    // Check if local session user is admin
    const isLocalAdmin = currentUser && (currentUser.role === 'ROLE_ADMIN' || currentUser.role === 'ADMIN' || (currentUser.role && currentUser.role.toUpperCase().includes('ADMIN')) || currentUser.displayRole === 'ADMIN' || (currentUser.permissions && currentUser.permissions.includes('ROLE_MANAGE')));
    if (isLocalAdmin) {
      const adminHeaderName = document.getElementById('adminHeaderName');
      if (adminHeaderName) adminHeaderName.textContent = currentUser.fullName;
      return true;
    }

    // Unauthorized - redirect to login page immediately
    removeCurrentUser();
    window.location.replace('index.html?error=unauthorized');
    return false;
  };

  const hasAccess = await verifyAdminAccess();
  if (!hasAccess) return;

  const adminLogoutBtn = document.getElementById('adminLogoutBtn');
  if (adminLogoutBtn) {
    adminLogoutBtn.addEventListener('click', async () => {
      try {
        await fetch(`${API_BASE}/auth/logout`, { method: 'POST' });
      } catch (e) {}
      removeCurrentUser();
      showToast('Logged out of Admin Control Center.', 'info');
      setTimeout(() => {
        window.location.href = 'index.html';
      }, 500);
    });
  }

  // Navigation Pill Active State & Smooth Scroll Handling
  const allNavPills = document.querySelectorAll('.admin-nav-pill, .mobile-nav-pill');
  allNavPills.forEach(pill => {
    pill.addEventListener('click', function(e) {
      const href = this.getAttribute('href');
      if (href && href.startsWith('#')) {
        allNavPills.forEach(p => {
          if (p.getAttribute('href') === href) p.classList.add('active');
          else if (p.getAttribute('href') && p.getAttribute('href').startsWith('#')) p.classList.remove('active');
        });
      }
    });
  });

  const updateStats = async () => {
    try {
      const res = await fetch(`${API_BASE}/stats`);
      if (!res.ok) return;
      const json = await res.json();
      const stats = json.data;
      if (!stats) return;

      const statUsers = document.getElementById('statRegisteredUsers');
      const statEmps = document.getElementById('statTotalEmployees');
      const statDepts = document.getElementById('statTotalDepartments');
      const statAtt = document.getElementById('statAttendanceRate');
      const statInq = document.getElementById('statPendingInquiries');
      const statPresent = document.getElementById('statPresentToday');
      const statAbsent = document.getElementById('statAbsentToday');

      if (statUsers) statUsers.textContent = stats.registeredUsers;
      if (statEmps) statEmps.textContent = stats.totalEmployees;
      if (statDepts) statDepts.textContent = stats.totalDepartments;
      if (statAtt) statAtt.textContent = `${stats.attendanceRate}%`;
      if (statInq) statInq.textContent = stats.pendingInquiries;
      if (statPresent) statPresent.textContent = stats.presentEmployeesToday != null ? stats.presentEmployeesToday : 0;
      if (statAbsent) statAbsent.textContent = stats.absentEmployeesToday != null ? stats.absentEmployeesToday : 0;
    } catch (e) {
      console.warn('Stats fetch error:', e);
    }
  };

  // Department-to-Role Dynamic Mapping
  const DEPARTMENT_ROLES = {
    "Engineering & Technology": [
      "Software Engineer",
      "Senior Java Developer",
      "Frontend Developer",
      "Backend Developer",
      "Full Stack Engineer",
      "DevOps & Cloud Engineer",
      "QA Automation Engineer",
      "UI/UX Designer",
      "Systems Architect"
    ],
    "Human Resources (HR)": [
      "HR Specialist",
      "Talent Acquisition Lead",
      "HR Generalist",
      "People Operations Associate",
      "HR Business Partner",
      "Employee Relations Officer",
      "Training & Development Coordinator"
    ],
    "Sales & Business": [
      "Business Development Manager",
      "Account Executive",
      "Sales Representative",
      "Enterprise Solutions Consultant",
      "Lead Generation Specialist",
      "Client Relationship Manager"
    ],
    "Finance & Accounts": [
      "Financial Analyst",
      "Senior Accountant",
      "Payroll Specialist",
      "Billing & Revenue Analyst",
      "Corporate Auditor",
      "Finance Operations Lead"
    ],
    "Marketing & Communications": [
      "Digital Marketing Specialist",
      "Content Strategist",
      "SEO & Growth Specialist",
      "Social Media Coordinator",
      "Brand Communications Lead",
      "Public Relations Specialist"
    ],
    "Operations & QA": [
      "Operations Coordinator",
      "Quality Assurance Analyst",
      "Logistics Specialist",
      "Compliance Officer",
      "Process Improvement Lead",
      "Facilities & Support Specialist"
    ]
  };

  const getRolesForDepartment = (deptName) => {
    if (!deptName) return DEPARTMENT_ROLES["Engineering & Technology"];
    for (const [key, roles] of Object.entries(DEPARTMENT_ROLES)) {
      if (key.toLowerCase() === deptName.toLowerCase() ||
          deptName.toLowerCase().includes(key.toLowerCase()) ||
          key.toLowerCase().includes(deptName.toLowerCase())) {
        return roles;
      }
    }
    return DEPARTMENT_ROLES["Engineering & Technology"];
  };

  const updateRolesSelect = (deptSelectId, titleSelectId, selectedRole = '') => {
    const deptEl = document.getElementById(deptSelectId);
    const titleEl = document.getElementById(titleSelectId);
    if (!deptEl || !titleEl) return;
    const roles = getRolesForDepartment(deptEl.value);
    titleEl.innerHTML = roles.map(r => `<option value="${escapeHtml(r)}">${escapeHtml(r)}</option>`).join('');
    if (selectedRole && roles.includes(selectedRole)) {
      titleEl.value = selectedRole;
    } else if (selectedRole) {
      titleEl.innerHTML += `<option value="${escapeHtml(selectedRole)}">${escapeHtml(selectedRole)}</option>`;
      titleEl.value = selectedRole;
    }
  };

  const registeredUsersTableBody = document.getElementById('registeredUsersTableBody');
  let registeredUsersCache = [];

  const renderUsersTable = async () => {
    if (!registeredUsersTableBody) return;
    try {
      const res = await fetch(`${API_BASE}/auth/users`);
      if (!res.ok) throw new Error('Users fetch error');
      const json = await res.json();
      registeredUsersCache = json.data || [];

      if (registeredUsersCache.length === 0) {
        registeredUsersTableBody.innerHTML = `
          <tr>
            <td colspan="5" style="text-align: center; padding: 24px; color: #64748b;">
              No registered user accounts found in database.
            </td>
          </tr>
        `;
        return;
      }

      registeredUsersTableBody.innerHTML = registeredUsersCache.map(user => {
        const role = user.role || 'ROLE_USER';
        let badgeBg = '#e0f2fe';
        let badgeColor = '#0284c7';
        let roleIcon = '👤';

        if (user.isAdmin || role === 'ROLE_ADMIN' || role === 'ADMIN') {
          badgeBg = '#fee2e2';
          badgeColor = '#dc2626';
          roleIcon = '👑';
        } else if (role === 'ROLE_HR_MANAGER') {
          badgeBg = '#ede9fe';
          badgeColor = '#7c3aed';
          roleIcon = '💼';
        } else if (role === 'ROLE_MANAGER') {
          badgeBg = '#e0e7ff';
          badgeColor = '#4338ca';
          roleIcon = '🏢';
        } else if (role === 'ROLE_VIEWER') {
          badgeBg = '#f1f5f9';
          badgeColor = '#475569';
          roleIcon = '👁️';
        }

        const displayRoleTxt = user.displayRole || (user.isAdmin ? 'Administrator' : 'Standard Employee');

        return `
        <tr>
          <td>
            <div style="display: flex; align-items: center; gap: 10px;">
              <div style="width: 32px; height: 32px; border-radius: 50%; background: ${badgeBg}; color: ${badgeColor}; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px;">
                ${escapeHtml(user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U')}
              </div>
              <div>
                <a href="javascript:void(0)" onclick="viewUserAttendanceHistory('${escapeHtml(user.email)}', '${escapeHtml(user.fullName || user.email)}')" style="color:var(--text-main); font-weight:700; text-decoration:underline; cursor:pointer;" title="Click to view full shift & attendance history">
                  ${escapeHtml(user.fullName || 'User')}
                </a>
              </div>
            </div>
          </td>
          <td><code>${escapeHtml(user.email)}</code></td>
          <td>
            <span style="display:inline-flex; align-items:center; gap:4px; padding:3px 10px; border-radius:12px; font-size:11.5px; font-weight:700; background:${badgeBg}; color:${badgeColor}; text-transform:uppercase;">
              <span>${roleIcon}</span> ${escapeHtml(displayRoleTxt)}
            </span>
          </td>
          <td>${escapeHtml(user.department || 'General')}</td>
          <td style="text-align: right;">
            <div style="display:flex; gap:6px; justify-content:flex-end; align-items:center;">
              <button type="button" class="btn-sm-edit" style="background:#f0fdf4; border-color:#86efac; color:#166534; font-weight:600; display:inline-flex; align-items:center; gap:4px;" onclick="viewUserAttendanceHistory('${escapeHtml(user.email)}', '${escapeHtml(user.fullName || user.email)}')" title="View Shift & Attendance History">⏱️ Attendance</button>
              ${user.email === 'admin@empmanage.com' 
                ? '<span style="font-size: 12px; color: #94a3b8; font-style: italic; margin-left: 4px;">Protected Master Admin</span>'
                : `
                  <button class="btn-sm-edit" onclick="openChangeUserRoleModal('${escapeHtml(user.email)}')" title="Change User Role">✏️ Change Role</button>
                  <a href="roles-permissions.html" class="btn-sm-edit" style="text-decoration:none;" title="View Permission Matrix">🛡️ Permissions</a>
                  <button class="btn-sm-delete" onclick="deleteUserAccount('${escapeHtml(user.email)}')" title="Delete Account">Delete</button>
                `
              }
            </div>
          </td>
        </tr>`;
      }).join('');
    } catch (e) {
      registeredUsersTableBody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:#ef4444; padding:20px;">Connection error loading users.</td></tr>`;
    }
  };

  // Modal 1C: Change User Role & Department
  const adminChangeUserRoleModalBackdrop = document.getElementById('adminChangeUserRoleModalBackdrop');
  const closeAdminChangeUserRoleModal = document.getElementById('closeAdminChangeUserRoleModal');
  const cancelAdminChangeUserRoleBtn = document.getElementById('cancelAdminChangeUserRoleBtn');
  const adminChangeUserRoleForm = document.getElementById('adminChangeUserRoleForm');
  const adminChangeUserDeptSelect = document.getElementById('adminChangeUserDeptSelect');

  const closeAdminChangeUserRoleModalFunc = () => {
    if (adminChangeUserRoleModalBackdrop) {
      adminChangeUserRoleModalBackdrop.classList.remove('open');
      document.body.style.overflow = '';
    }
  };

  if (closeAdminChangeUserRoleModal) closeAdminChangeUserRoleModal.addEventListener('click', closeAdminChangeUserRoleModalFunc);
  if (cancelAdminChangeUserRoleBtn) cancelAdminChangeUserRoleBtn.addEventListener('click', closeAdminChangeUserRoleModalFunc);
  if (adminChangeUserRoleModalBackdrop) {
    adminChangeUserRoleModalBackdrop.addEventListener('click', (e) => {
      if (e.target === adminChangeUserRoleModalBackdrop) closeAdminChangeUserRoleModalFunc();
    });
  }

  if (adminChangeUserDeptSelect) {
    adminChangeUserDeptSelect.addEventListener('change', () => {
      updateRolesSelect('adminChangeUserDeptSelect', 'adminChangeUserTitleSelect');
    });
  }

  window.openChangeUserRoleModal = (email) => {
    const user = registeredUsersCache.find(u => u.email.toLowerCase() === email.toLowerCase());
    if (!user) {
      showToast('User account not found.', 'error');
      return;
    }

    document.getElementById('adminChangeUserEmail').value = user.email;
    document.getElementById('adminChangeUserNameDisplay').value = `${user.fullName} (${user.email})`;
    
    const roleSelect = document.getElementById('adminChangeUserRoleSelect');
    if (roleSelect) {
      roleSelect.value = user.role || 'ROLE_USER';
    }

    const deptSelect = document.getElementById('adminChangeUserDeptSelect');
    if (deptSelect && user.department) {
      deptSelect.value = user.department;
    }

    updateRolesSelect('adminChangeUserDeptSelect', 'adminChangeUserTitleSelect');

    if (adminChangeUserRoleModalBackdrop) {
      adminChangeUserRoleModalBackdrop.classList.add('open');
      document.body.style.overflow = 'hidden';
    }
  };

  if (adminChangeUserRoleForm) {
    adminChangeUserRoleForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('adminChangeUserEmail').value;
      const role = document.getElementById('adminChangeUserRoleSelect').value;
      const department = document.getElementById('adminChangeUserDeptSelect').value;
      const title = document.getElementById('adminChangeUserTitleSelect').value;

      try {
        const res = await fetch(`${API_BASE}/auth/users/${encodeURIComponent(email)}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ role, department, title })
        });
        const json = await res.json();

        if (res.ok && json.success) {
          showToast(`Account updated! Role is now ${role.includes('ADMIN') ? 'Admin' : 'Employee'}.`, 'success');
          closeAdminChangeUserRoleModalFunc();
          renderUsersTable();
          renderAdminEmployeesTable();
          loadAdminDepartments();
          updateStats();
        } else {
          showToast(json.message || 'Error updating user role.', 'error');
        }
      } catch (err) {
        showToast('Connection error updating user account.', 'error');
      }
    });
  }

  window.deleteUserAccount = async (email) => {
    if (!confirm(`Are you sure you want to delete user account "${email}"? This will also remove all associated attendance records.`)) return;
    try {
      const res = await fetch(`${API_BASE}/auth/users/${encodeURIComponent(email)}`, { method: 'DELETE' });
      const json = await res.json();
      if (res.ok) {
        showToast(json.message || `Account "${email}" and attendance records removed.`, 'info');
        renderUsersTable();
        loadAttendanceLogs();
        updateStats();
      } else {
        showToast(json.message || 'Error deleting account.', 'error');
      }
    } catch (e) {
      showToast('Connection error deleting user.', 'error');
    }
  };

  const adminEmployeesTableBody = document.getElementById('adminEmployeesTableBody');
  const adminEmpSearchInput = document.getElementById('adminEmpSearchInput');

  const renderAdminEmployeesTable = async () => {
    if (!adminEmployeesTableBody) return;

    if (!currentUser) currentUser = getCurrentUser();

    // 1. Dynamic check for EMPLOYEE_VIEW permission
    const canView = hasPermission('EMPLOYEE_VIEW');
    const canCreate = hasPermission('EMPLOYEE_CREATE');
    const canEdit = hasPermission('EMPLOYEE_EDIT');
    const canDelete = hasPermission('EMPLOYEE_DELETE');
    const canExport = hasPermission('EMPLOYEE_EXPORT');

    // Toggle button visibility based on granular permissions
    if (openAddEmpFromAdminBtn) {
      openAddEmpFromAdminBtn.style.display = canCreate ? 'inline-flex' : 'none';
    }
    const exportEmployeesBtn = document.querySelector('a[href="/api/reports/employees/csv"]');
    if (exportEmployeesBtn) {
      exportEmployeesBtn.style.display = canExport ? 'inline-flex' : 'none';
    }

    if (!canView) {
      adminEmployeesTableBody.innerHTML = `
        <tr>
          <td colspan="6" style="text-align: center; padding: 48px 20px; color: #ef4444;">
            <div style="font-size: 32px; margin-bottom: 8px;">🔒</div>
            <div style="font-size: 15px; font-weight: 700; color: #0f172a;">Workforce Directory Access Restricted</div>
            <div style="font-size: 13px; color: #64748b; margin-top: 4px;">
              Your current role does not possess the <strong>EMPLOYEE_VIEW</strong> permission.<br>
              To view company workforce records, grant this capability in <strong>Role &amp; Permission Studio</strong>.
            </div>
          </td>
        </tr>
      `;
      return;
    }

    const search = (adminEmpSearchInput ? adminEmpSearchInput.value : '').trim();

    try {
      const url = `${API_BASE}/employees?query=${encodeURIComponent(search)}`;
      const res = await fetch(url);
      if (!res.ok) {
        if (res.status === 403) {
          adminEmployeesTableBody.innerHTML = `
            <tr>
              <td colspan="6" style="text-align: center; padding: 36px 20px; color: #ef4444; font-weight: 600;">
                🔒 Access Restricted by Security Policy (EMPLOYEE_VIEW required).
              </td>
            </tr>
          `;
          return;
        }
        throw new Error('Employees fetch error');
      }
      const json = await res.json();
      const rawData = json.data;
      const emps = Array.isArray(rawData) ? rawData : (rawData?.content || rawData?.employees || []);

      if (emps.length === 0) {
        adminEmployeesTableBody.innerHTML = `
          <tr>
            <td colspan="6" style="text-align: center; padding: 36px 20px; color: #64748b;">
              No workforce employees found. Click <strong>"+ Add Employee"</strong> to register team members.
            </td>
          </tr>
        `;
        return;
      }

      adminEmployeesTableBody.innerHTML = emps.map(emp => {
        const deptName = emp.departmentName || (emp.department && emp.department.name) || (typeof emp.department === 'string' ? emp.department : '') || 'General';
        
        let actionButtons = `
          <button type="button" class="btn-sm-edit" style="background:#f0fdf4; border-color:#86efac; color:#166534; font-weight:600; display:inline-flex; align-items:center; gap:4px;" onclick="viewUserAttendanceHistory('${escapeHtml(emp.email)}', '${escapeHtml(emp.name)}')" title="View Employee Attendance History">⏱️ Attendance</button>
        `;
        if (canEdit) {
          actionButtons += `<button class="btn-sm-edit" onclick="editEmpFromAdmin(${emp.id})">Edit</button>`;
        }
        if (canDelete) {
          actionButtons += `<button class="btn-sm-delete" onclick="deleteEmpFromAdmin(${emp.id})">Delete</button>`;
        }
        if (!canEdit && !canDelete) {
          actionButtons += `<span style="font-size: 11.5px; color: #94a3b8; font-weight: 600; padding: 3px 8px; background: #f1f5f9; border-radius: 4px;">Read-Only</span>`;
        }

        return `
        <tr>
          <td>
            <div style="display:flex; align-items:center; gap:8px;">
              <a href="javascript:void(0)" onclick="viewUserAttendanceHistory('${escapeHtml(emp.email)}', '${escapeHtml(emp.name)}')" style="color:var(--text-main); font-weight:700; text-decoration:underline; cursor:pointer;" title="Click to view full shift & attendance history">
                ${escapeHtml(emp.name)}
              </a>
              <span style="font-size:11px; color:#64748b; font-family:monospace;">(${escapeHtml(emp.employeeCode || '')})</span>
            </div>
          </td>
          <td>${escapeHtml(emp.title)}</td>
          <td><span style="font-weight:600; color:#334155;">${escapeHtml(deptName)}</span></td>
          <td><a href="mailto:${escapeHtml(emp.email)}" style="color: var(--primary);">${escapeHtml(emp.email)}</a></td>
          <td><span class="status-badge active">${escapeHtml(emp.status || 'Active')}</span></td>
          <td style="text-align: right;">
            <div style="display:flex; gap:6px; justify-content:flex-end; align-items:center;">
              ${actionButtons}
            </div>
          </td>
        </tr>
      `}).join('');
    } catch (e) {
      adminEmployeesTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:#ef4444; padding:20px;">Connection error loading employees.</td></tr>`;
    }
  };

  if (adminEmpSearchInput) {
    adminEmpSearchInput.addEventListener('input', renderAdminEmployeesTable);
  }

  window.deleteEmpFromAdmin = async (id) => {
    if (!hasPermission('EMPLOYEE_DELETE')) {
      showToast('Access Denied: Your role does not have permission to delete employees (EMPLOYEE_DELETE required).', 'error');
      return;
    }
    if (!confirm('Are you sure you want to remove this employee from company workforce?')) return;
    try {
      const res = await fetch(`${API_BASE}/employees/${id}`, { method: 'DELETE' });
      const json = await res.json();
      if (res.ok && json.success) {
        showToast(json.message || 'Employee removed successfully.', 'info');
        renderAdminEmployeesTable();
        loadAttendanceLogs();
        loadAdminDepartments();
        updateStats();
      } else {
        showToast(json.message || 'Error removing employee: Permission denied.', 'error');
      }
    } catch (e) {
      showToast('Connection error removing employee.', 'error');
    }
  };

  // Modal 1: Add Employee
  const adminAddEmpModalBackdrop = document.getElementById('adminAddEmpModalBackdrop');
  const openAddEmpFromAdminBtn = document.getElementById('openAddEmpFromAdminBtn');
  const closeAdminAddEmpModal = document.getElementById('closeAdminAddEmpModal');
  const cancelAdminAddEmpBtn = document.getElementById('cancelAdminAddEmpBtn');
  const adminAddEmpForm = document.getElementById('adminAddEmpForm');

  const openAdminAddEmpModalFunc = () => {
    if (adminAddEmpModalBackdrop) {
      updateRolesSelect('adminNewEmpDept', 'adminNewEmpTitle');
      adminAddEmpModalBackdrop.classList.add('open');
      document.body.style.overflow = 'hidden';
    }
  };

  const closeAdminAddEmpModalFunc = () => {
    if (adminAddEmpModalBackdrop) {
      adminAddEmpModalBackdrop.classList.remove('open');
      document.body.style.overflow = '';
    }
  };

  if (openAddEmpFromAdminBtn) openAddEmpFromAdminBtn.addEventListener('click', openAdminAddEmpModalFunc);
  if (closeAdminAddEmpModal) closeAdminAddEmpModal.addEventListener('click', closeAdminAddEmpModalFunc);
  if (cancelAdminAddEmpBtn) cancelAdminAddEmpBtn.addEventListener('click', closeAdminAddEmpModalFunc);
  if (adminAddEmpModalBackdrop) {
    adminAddEmpModalBackdrop.addEventListener('click', (e) => {
      if (e.target === adminAddEmpModalBackdrop) closeAdminAddEmpModalFunc();
    });
  }

  const adminNewEmpDeptInput = document.getElementById('adminNewEmpDept');
  if (adminNewEmpDeptInput) {
    adminNewEmpDeptInput.addEventListener('change', () => {
      updateRolesSelect('adminNewEmpDept', 'adminNewEmpTitle');
    });
  }

  const adminEditEmpDeptInput = document.getElementById('adminEditEmpDept');
  if (adminEditEmpDeptInput) {
    adminEditEmpDeptInput.addEventListener('change', () => {
      updateRolesSelect('adminEditEmpDept', 'adminEditEmpTitle');
    });
  }

  if (adminAddEmpForm) {
    adminAddEmpForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (!hasPermission('EMPLOYEE_CREATE')) {
        showToast('Access Denied: Your active role does not have permission to add employees (EMPLOYEE_CREATE required).', 'error');
        return;
      }
      const name = document.getElementById('adminNewEmpName').value.trim();
      const title = document.getElementById('adminNewEmpTitle').value.trim();
      const dept = document.getElementById('adminNewEmpDept').value;
      const email = document.getElementById('adminNewEmpEmail').value.trim().toLowerCase();
      const phoneInput = document.getElementById('adminNewEmpPhone');
      const phone = phoneInput ? phoneInput.value.trim() : '';

      try {
        const res = await fetch(`${API_BASE}/employees`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name, title, department: dept, email, phone, status: 'Active' })
        });
        const json = await res.json();

        if (res.ok && json.success) {
          showToast(`Employee "${name}" registered successfully! Attendance tracking initialized.`, 'success');
          closeAdminAddEmpModalFunc();
          adminAddEmpForm.reset();
          renderAdminEmployeesTable();
          loadAttendanceLogs();
          loadAdminDepartments();
          updateStats();
        } else {
          showToast(json.message || 'Error adding employee.', 'error');
        }
      } catch (err) {
        showToast('Connection error registering employee.', 'error');
      }
    });
  }

  // Modal 1B: Edit Employee
  const adminEditEmpModalBackdrop = document.getElementById('adminEditEmpModalBackdrop');
  const closeAdminEditEmpModal = document.getElementById('closeAdminEditEmpModal');
  const cancelAdminEditEmpBtn = document.getElementById('cancelAdminEditEmpBtn');
  const adminEditEmpForm = document.getElementById('adminEditEmpForm');

  const closeAdminEditEmpModalFunc = () => {
    if (adminEditEmpModalBackdrop) {
      adminEditEmpModalBackdrop.classList.remove('open');
      document.body.style.overflow = '';
    }
  };

  if (closeAdminEditEmpModal) closeAdminEditEmpModal.addEventListener('click', closeAdminEditEmpModalFunc);
  if (cancelAdminEditEmpBtn) cancelAdminEditEmpBtn.addEventListener('click', closeAdminEditEmpModalFunc);
  if (adminEditEmpModalBackdrop) {
    adminEditEmpModalBackdrop.addEventListener('click', (e) => {
      if (e.target === adminEditEmpModalBackdrop) closeAdminEditEmpModalFunc();
    });
  }

  window.editEmpFromAdmin = async (id) => {
    if (!hasPermission('EMPLOYEE_EDIT')) {
      showToast('Access Denied: Your active role does not have permission to edit employees (EMPLOYEE_EDIT required).', 'error');
      return;
    }
    try {
      const res = await fetch(`${API_BASE}/employees/${id}`);
      if (!res.ok) throw new Error('Employee not found');
      const json = await res.json();
      const emp = json.data;
      if (!emp) throw new Error('Employee data is missing');

      document.getElementById('adminEditEmpId').value = emp.id;
      document.getElementById('adminEditEmpName').value = emp.name || '';
      document.getElementById('adminEditEmpEmail').value = emp.email || '';
      document.getElementById('adminEditEmpPhone').value = emp.phone || '';

      const deptName = emp.departmentName || (emp.department && emp.department.name) || (typeof emp.department === 'string' ? emp.department : '');
      const deptSelect = document.getElementById('adminEditEmpDept');
      if (deptSelect && deptName) {
        deptSelect.value = deptName;
      }

      updateRolesSelect('adminEditEmpDept', 'adminEditEmpTitle', emp.title || '');

      const statusSelect = document.getElementById('adminEditEmpStatus');
      if (statusSelect && emp.status) {
        statusSelect.value = emp.status;
      }

      if (adminEditEmpModalBackdrop) {
        adminEditEmpModalBackdrop.classList.add('open');
        document.body.style.overflow = 'hidden';
      }
    } catch (e) {
      showToast('Error loading employee details for editing.', 'error');
    }
  };

  if (adminEditEmpForm) {
    adminEditEmpForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (!hasPermission('EMPLOYEE_EDIT')) {
        showToast('Access Denied: Your active role does not have permission to edit employees (EMPLOYEE_EDIT required).', 'error');
        return;
      }
      const id = document.getElementById('adminEditEmpId').value;
      const name = document.getElementById('adminEditEmpName').value.trim();
      const title = document.getElementById('adminEditEmpTitle').value.trim();
      const dept = document.getElementById('adminEditEmpDept').value;
      const email = document.getElementById('adminEditEmpEmail').value.trim().toLowerCase();
      const phone = document.getElementById('adminEditEmpPhone').value.trim();
      const status = document.getElementById('adminEditEmpStatus').value;

      try {
        const res = await fetch(`${API_BASE}/employees/${id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name, title, department: dept, email, phone, status })
        });
        const json = await res.json();

        if (res.ok && json.success) {
          showToast(`Employee "${name}" updated successfully!`, 'success');
          closeAdminEditEmpModalFunc();
          renderAdminEmployeesTable();
          loadAttendanceLogs();
          loadAdminDepartments();
          updateStats();
        } else {
          showToast(json.message || 'Error updating employee record.', 'error');
        }
      } catch (err) {
        showToast('Connection error updating employee.', 'error');
      }
    });
  }
  const adminAttendanceHistoryTableBody = document.getElementById('adminAttendanceHistoryTableBody');
  const adminAttStaffSelect = document.getElementById('adminAttStaffSelect');
  const adminAttDeptFilter = document.getElementById('adminAttDeptFilter');
  const adminAttStatusFilter = document.getElementById('adminAttStatusFilter');
  const adminAttDateFilter = document.getElementById('adminAttDateFilter');
  const adminAttSearchInput = document.getElementById('adminAttSearchInput');
  const adminAttSummaryBanner = document.getElementById('adminAttSummaryBanner');
  const adminAttSummaryText = document.getElementById('adminAttSummaryText');

  // Admin Live Clock & Shift Punch controls
  const adminLiveClockDisplay = document.getElementById('adminLiveClockDisplay');
  const adminPunchClockBtn = document.getElementById('adminPunchClockBtn');
  const adminPunchIcon = document.getElementById('adminPunchIcon');
  const adminPunchText = document.getElementById('adminPunchText');

  const updateAdminLiveClock = () => {
    if (adminLiveClockDisplay) {
      adminLiveClockDisplay.textContent = new Date().toLocaleTimeString('en-US', { hour12: true, hour: '2-digit', minute: '2-digit', second: '2-digit' });
    }
  };
  setInterval(updateAdminLiveClock, 1000);
  updateAdminLiveClock();

  let allAttendanceLogsCache = [];

  const updateAdminPunchButtonState = () => {
    if (!adminPunchClockBtn || !currentUser) return;
    const now = new Date();
    const todayKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    const myLog = allAttendanceLogsCache.find(l => l.userEmail && l.userEmail.toLowerCase() === currentUser.email.toLowerCase() && l.dateKey === todayKey);

    if (myLog && myLog.punchedIn) {
      if (adminPunchIcon) adminPunchIcon.textContent = '🔴';
      if (adminPunchText) adminPunchText.textContent = `Punch Out (${myLog.timeIn})`;
      adminPunchClockBtn.style.backgroundColor = '#dc2626';
      adminPunchClockBtn.style.borderColor = '#dc2626';
    } else {
      if (adminPunchIcon) adminPunchIcon.textContent = '🟢';
      if (myLog && myLog.timeOut && myLog.timeOut !== '--:--' && myLog.timeOut !== 'Ongoing Shift') {
        if (adminPunchText) adminPunchText.textContent = `Punched Out (${myLog.timeOut})`;
      } else {
        if (adminPunchText) adminPunchText.textContent = 'Punch In';
      }
      adminPunchClockBtn.style.backgroundColor = '';
      adminPunchClockBtn.style.borderColor = '';
    }
  };

  if (adminPunchClockBtn) {
    adminPunchClockBtn.addEventListener('click', async () => {
      if (!currentUser) return;
      const punchNow = new Date();
      const formattedPunchTime = punchNow.toLocaleTimeString('en-US', { hour12: true, hour: '2-digit', minute: '2-digit', second: '2-digit' });

      try {
        const res = await fetch(`${API_BASE}/attendance/punch`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            userEmail: currentUser.email,
            mode: 'Office',
            punchTime: formattedPunchTime
          })
        });
        const json = await res.json();
        if (res.ok && json.success) {
          showToast(json.message, 'success');
          await loadAttendanceLogs();
          updateStats();
        } else {
          showToast(json.message || 'Error recording attendance.', 'error');
        }
      } catch (err) {
        showToast('Connection error communicating with attendance API.', 'error');
      }
    });
  }

  const loadAttendanceLogs = async () => {
    try {
      const res = await fetch(`${API_BASE}/attendance/all`);
      if (!res.ok) throw new Error('Attendance fetch error');
      const json = await res.json();
      allAttendanceLogsCache = json.data || [];

      updateAdminPunchButtonState();
      populateStaffAndDeptAttendanceFilters();
      filterAndRenderAttendanceTable();
    } catch (e) {
      if (adminAttendanceHistoryTableBody) {
        adminAttendanceHistoryTableBody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:#ef4444; padding:20px;">Connection error loading attendance logs.</td></tr>`;
      }
    }
  };

  const populateStaffAndDeptAttendanceFilters = () => {
    if (!adminAttStaffSelect) return;
    const staffMap = new Map();

    allAttendanceLogsCache.forEach(l => {
      if (l.userEmail && !staffMap.has(l.userEmail.toLowerCase())) {
        staffMap.set(l.userEmail.toLowerCase(), {
          email: l.userEmail,
          name: l.userName || l.userEmail
        });
      }
    });

    const currentVal = adminAttStaffSelect.value || 'ALL';
    let optionsHtml = `<option value="ALL">👤 All Staff Members (${staffMap.size})</option>`;
    staffMap.forEach(staff => {
      optionsHtml += `<option value="${escapeHtml(staff.email)}">${escapeHtml(staff.name)} (${escapeHtml(staff.email)})</option>`;
    });

    adminAttStaffSelect.innerHTML = optionsHtml;
    adminAttStaffSelect.value = currentVal;
  };

  const filterAndRenderAttendanceTable = () => {
    if (!adminAttendanceHistoryTableBody) return;

    const staffFilter = (adminAttStaffSelect ? adminAttStaffSelect.value : 'ALL').toLowerCase();
    const deptFilter = (adminAttDeptFilter ? adminAttDeptFilter.value : 'ALL').toLowerCase();
    const statusFilter = (adminAttStatusFilter ? adminAttStatusFilter.value : 'ALL');
    const dateFilter = (adminAttDateFilter ? adminAttDateFilter.value : 'ALL');
    const search = (adminAttSearchInput ? adminAttSearchInput.value : '').toLowerCase().trim();

    const now = new Date();
    const todayKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

    const filtered = allAttendanceLogsCache.filter(item => {
      const matchStaff = (staffFilter === 'all' || (item.userEmail && item.userEmail.toLowerCase() === staffFilter));
      const matchDept = (deptFilter === 'all' || (item.department && item.department.toLowerCase().includes(deptFilter)));
      const matchStatus = (statusFilter === 'ALL' || item.status === statusFilter);
      const matchDate = (dateFilter === 'ALL' || (dateFilter === 'TODAY' && item.dateKey === todayKey));
      const matchSearch = (!search ||
        (item.userName && item.userName.toLowerCase().includes(search)) ||
        (item.userEmail && item.userEmail.toLowerCase().includes(search)) ||
        (item.department && item.department.toLowerCase().includes(search)) ||
        (item.date && item.date.toLowerCase().includes(search)));

      return matchStaff && matchDept && matchStatus && matchDate && matchSearch;
    });
    if (staffFilter !== 'all') {
      const staffLogs = allAttendanceLogsCache.filter(h => h.userEmail && h.userEmail.toLowerCase() === staffFilter);
      const presentCount = staffLogs.filter(h => h.status === 'Present' || h.status === 'WFH').length;
      const rate = Math.round((presentCount / (staffLogs.length || 1)) * 100);
      const staffObj = staffLogs[0];

      if (adminAttSummaryBanner && adminAttSummaryText) {
        adminAttSummaryBanner.style.display = 'flex';
        adminAttSummaryText.innerHTML = `Showing <strong>${staffLogs.length} shifts</strong> for <strong>${escapeHtml(staffObj ? staffObj.userName : staffFilter)} (${escapeHtml(staffFilter)})</strong> &bull; Attendance Rate: <strong>${rate}%</strong> &bull; Present: <strong>${presentCount}/${staffLogs.length} shifts</strong>`;
      }
    } else {
      if (adminAttSummaryBanner) adminAttSummaryBanner.style.display = 'none';
    }

    if (filtered.length === 0) {
      adminAttendanceHistoryTableBody.innerHTML = `
        <tr>
          <td colspan="8" style="text-align: center; padding: 36px 20px; color: #64748b;">
            No attendance records found matching filters.
          </td>
        </tr>
      `;
      return;
    }

    adminAttendanceHistoryTableBody.innerHTML = filtered.map(item => {
      let statusBg = '#dcfce7';
      let statusColor = '#15803d';
      if (item.status === 'WFH') {
        statusBg = '#dbeafe';
        statusColor = '#1d4ed8';
      } else if (item.status === 'On Leave') {
        statusBg = '#fef3c7';
        statusColor = '#b45309';
      } else if (item.status === 'Absent' || item.status === 'Not Punched In') {
        statusBg = '#fee2e2';
        statusColor = '#b91c1c';
      }

      const isToday = item.dateKey === todayKey;

      return `
        <tr>
          <td>
            <strong style="color:#0f172a;">${escapeHtml(item.date)}</strong>
            ${isToday ? '<span style="background: #e0f2fe; color: #0369a1; font-size: 10px; font-weight: 700; padding: 2px 6px; border-radius: 4px; margin-left: 6px;">TODAY</span>' : ''}
          </td>
          <td>
            <div style="display: flex; align-items: center; gap: 8px;">
              <div style="width: 32px; height: 32px; border-radius: 50%; background: #eff6ff; color: #1d4ed8; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 12px; flex-shrink: 0;">
                ${escapeHtml(item.userName ? item.userName.charAt(0).toUpperCase() : 'U')}
              </div>
              <div>
                <a href="javascript:void(0)" onclick="viewUserAttendanceHistory('${escapeHtml(item.userEmail)}', '${escapeHtml(item.userName)}')" style="font-weight: 700; color: var(--primary); text-decoration: underline;" title="View complete shift & attendance history">
                  ${escapeHtml(item.userName)}
                </a>
                <div style="font-size: 11px; color: #64748b; font-family: monospace;">${escapeHtml(item.userEmail)}</div>
              </div>
            </div>
          </td>
          <td><span style="font-size: 13px;">${escapeHtml(item.department || 'General')}</span></td>
          <td><code>${escapeHtml(item.timeIn)}</code></td>
          <td>
            ${(item.timeOut === 'Ongoing Shift' || (item.punchedIn && item.timeOut === '--:--'))
              ? `<span style="background: #fef3c7; color: #b45309; font-size: 11px; font-weight: 700; padding: 2px 7px; border-radius: 4px; display: inline-block;">Ongoing Shift</span>`
              : `<code>${escapeHtml(item.timeOut)}</code>`}
          </td>
          <td><span style="color: #475569; font-weight: 500;">${escapeHtml(item.hours)}</span></td>
          <td>
            <span style="display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 700; background: ${statusBg}; color: ${statusColor};">
              ${escapeHtml(item.status)}
            </span>
          </td>
          <td style="text-align: right; white-space: nowrap;">
            ${(item.timeOut === 'Ongoing Shift' || item.punchedIn) ? `
              <button type="button" class="btn btn-sm" onclick="openAdminPunchOutModal(${item.id})" style="background: #ea580c; color: #fff; border: none; padding: 3px 8px; font-size: 11.5px; border-radius: 4px; font-weight: 600; margin-right: 5px; vertical-align: middle; cursor: pointer; box-shadow: 0 1px 2px rgba(0,0,0,0.1);" title="Force punch out for ${escapeHtml(item.date)}">
                🚪 Punch Out
              </button>
            ` : ''}
            <select class="form-select" style="padding: 3px 8px; font-size: 11.5px; width: auto; display: inline-block; vertical-align: middle; margin-right: 6px;" onchange="changeAdminHistoryStatus(${item.id}, this.value)">
              <option value="Present" ${item.status === 'Present' ? 'selected' : ''}>Present</option>
              <option value="WFH" ${item.status === 'WFH' ? 'selected' : ''}>WFH</option>
              <option value="On Leave" ${item.status === 'On Leave' ? 'selected' : ''}>On Leave</option>
              <option value="Absent" ${item.status === 'Absent' ? 'selected' : ''}>Absent</option>
              <option value="Not Punched In" ${item.status === 'Not Punched In' ? 'selected' : ''}>Not Punched In</option>
            </select>
            <button class="btn-sm-delete" onclick="deleteAttendanceRecord(${item.id})" title="Delete attendance record" style="vertical-align: middle;">
              Delete
            </button>
          </td>
        </tr>
      `;
    }).join('');
  };

  window.filterStaffAttendance = (email) => {
    if (adminAttStaffSelect) {
      adminAttStaffSelect.value = email;
      filterAndRenderAttendanceTable();
      showToast(`Filtered attendance history for ${email}`, 'info');
    }
  };

  window.changeAdminHistoryStatus = async (id, newStatus) => {
    try {
      const res = await fetch(`${API_BASE}/attendance/${id}/status?status=${encodeURIComponent(newStatus)}`, {
        method: 'PUT'
      });
      const json = await res.json();
      if (res.ok) {
        showToast(`Attendance status updated to "${newStatus}".`, 'success');
        loadAttendanceLogs();
        updateStats();
        if (currentInspectedUserEmail && typeof renderInspectedUserAttendanceTable === 'function') {
          const item = currentInspectedUserLogs.find(l => l.id === id);
          if (item) {
            item.status = newStatus;
            renderInspectedUserAttendanceTable();
          }
        }
      } else {
        showToast(json.message || 'Status override failed.', 'error');
      }
    } catch (e) {
      showToast('Connection error updating status.', 'error');
    }
  };

  window.deleteAttendanceRecord = async (id) => {
    if (!confirm('Are you sure you want to delete this attendance log record?')) return;
    try {
      const res = await fetch(`${API_BASE}/attendance/${id}`, {
        method: 'DELETE'
      });
      const json = await res.json();
      if (res.ok && json.success) {
        showToast('Attendance record deleted successfully.', 'info');
        await loadAttendanceLogs();
        updateStats();
      } else {
        showToast(json.message || 'Error deleting attendance record.', 'error');
      }
    } catch (e) {
      showToast('Connection error deleting attendance record.', 'error');
    }
  };

  // Admin Shift Force Punch-Out Modal controls
  const adminPunchOutModalBackdrop = document.getElementById('adminPunchOutModalBackdrop');
  const closeAdminPunchOutModal = document.getElementById('closeAdminPunchOutModal');
  const cancelAdminPunchOutBtn = document.getElementById('cancelAdminPunchOutBtn');
  const adminPunchOutForm = document.getElementById('adminPunchOutForm');
  const adminPunchOutRecordId = document.getElementById('adminPunchOutRecordId');
  const adminPunchOutEmpName = document.getElementById('adminPunchOutEmpName');
  const adminPunchOutShiftDate = document.getElementById('adminPunchOutShiftDate');
  const adminPunchOutTimeIn = document.getElementById('adminPunchOutTimeIn');
  const adminPunchOutTimeInput = document.getElementById('adminPunchOutTimeInput');
  const adminPunchOutDateNote = document.getElementById('adminPunchOutDateNote');

  window.openAdminPunchOutModal = (id, empName, dateStr, timeInStr, dateKey) => {
    if (!adminPunchOutModalBackdrop) return;
    const item = allAttendanceLogsCache.find(l => l.id === Number(id));
    const effectiveName = (item && item.userName) ? item.userName : (empName || 'Employee');
    const effectiveDate = (item && item.date) ? item.date : (dateStr || 'Shift Date');
    const effectiveTimeIn = (item && item.timeIn) ? item.timeIn : (timeInStr || '--:--');
    const effectiveDateKey = (item && item.dateKey) ? item.dateKey : (dateKey || '');

    if (adminPunchOutRecordId) adminPunchOutRecordId.value = id;
    if (adminPunchOutEmpName) adminPunchOutEmpName.textContent = effectiveName;
    if (adminPunchOutShiftDate) adminPunchOutShiftDate.textContent = effectiveDate;
    if (adminPunchOutTimeIn) adminPunchOutTimeIn.textContent = effectiveTimeIn;
    if (adminPunchOutDateNote) adminPunchOutDateNote.textContent = effectiveDate;

    const todayStr = new Date().toISOString().substring(0, 10);
    if (adminPunchOutTimeInput) {
      if (effectiveDateKey === todayStr) {
        adminPunchOutTimeInput.value = new Date().toLocaleTimeString('en-US', { hour12: true, hour: '2-digit', minute: '2-digit', second: '2-digit' });
      } else {
        adminPunchOutTimeInput.value = '06:00:00 PM';
      }
    }

    adminPunchOutModalBackdrop.classList.add('open');
  };

  const closePunchOutModalFn = () => {
    if (adminPunchOutModalBackdrop) adminPunchOutModalBackdrop.classList.remove('open');
  };

  if (closeAdminPunchOutModal) closeAdminPunchOutModal.addEventListener('click', closePunchOutModalFn);
  if (cancelAdminPunchOutBtn) cancelAdminPunchOutBtn.addEventListener('click', closePunchOutModalFn);
  if (adminPunchOutModalBackdrop) {
    adminPunchOutModalBackdrop.addEventListener('click', (e) => {
      if (e.target === adminPunchOutModalBackdrop) closePunchOutModalFn();
    });
  }

  window.setPresetPunchOut = (val) => {
    if (!adminPunchOutTimeInput) return;
    if (val === 'NOW') {
      adminPunchOutTimeInput.value = new Date().toLocaleTimeString('en-US', { hour12: true, hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } else {
      adminPunchOutTimeInput.value = val;
    }
  };

  if (adminPunchOutForm) {
    adminPunchOutForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const id = adminPunchOutRecordId.value;
      const punchOutTime = adminPunchOutTimeInput.value.trim();
      if (!id || !punchOutTime) {
        showToast('Please specify a valid punch out time.', 'warning');
        return;
      }

      try {
        const res = await fetch(`${API_BASE}/attendance/${id}/admin-punch-out`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ punchOutTime })
        });
        const json = await res.json();
        if (res.ok && json.success) {
          showToast(json.message || 'Shift punched out successfully!', 'success');
          closePunchOutModalFn();
          await loadAttendanceLogs();
          updateStats();
          if (currentInspectedUserEmail && typeof window.viewUserAttendanceHistory === 'function') {
            window.viewUserAttendanceHistory(currentInspectedUserEmail, currentInspectedUserName);
          }
        } else {
          showToast(json.message || 'Error executing punch out.', 'error');
        }
      } catch (err) {
        showToast('Connection error performing punch out.', 'error');
      }
    });
  }

  const clearStaffFilterBtn = document.getElementById('clearStaffFilterBtn');
  if (clearStaffFilterBtn) {
    clearStaffFilterBtn.addEventListener('click', () => {
      if (adminAttStaffSelect) adminAttStaffSelect.value = 'ALL';
      filterAndRenderAttendanceTable();
    });
  }

  if (adminAttStaffSelect) adminAttStaffSelect.addEventListener('change', filterAndRenderAttendanceTable);
  if (adminAttDeptFilter) adminAttDeptFilter.addEventListener('change', filterAndRenderAttendanceTable);
  if (adminAttStatusFilter) adminAttStatusFilter.addEventListener('change', filterAndRenderAttendanceTable);
  if (adminAttDateFilter) adminAttDateFilter.addEventListener('change', filterAndRenderAttendanceTable);
  if (adminAttSearchInput) adminAttSearchInput.addEventListener('input', filterAndRenderAttendanceTable);
  const adminDepartmentsTableBody = document.getElementById('adminDepartmentsTableBody');
  const openAddDeptModalBtn = document.getElementById('openAddDeptModalBtn');
  const adminAddDeptModalBackdrop = document.getElementById('adminAddDeptModalBackdrop');
  const closeAdminAddDeptModal = document.getElementById('closeAdminAddDeptModal');
  const cancelAdminAddDeptBtn = document.getElementById('cancelAdminAddDeptBtn');
  const adminAddDeptForm = document.getElementById('adminAddDeptForm');
  const adminNewEmpDept = document.getElementById('adminNewEmpDept');

  const loadAdminDepartments = async () => {
    try {
      const res = await fetch(`${API_BASE}/departments`);
      if (!res.ok) throw new Error('Departments fetch error');
      const json = await res.json();
      const depts = json.data || [];
      if (adminNewEmpDept) {
        adminNewEmpDept.innerHTML = depts.map(d => `<option value="${escapeHtml(d.name)}">${escapeHtml(d.name)}</option>`).join('');
      }
      const adminEditEmpDept = document.getElementById('adminEditEmpDept');
      if (adminEditEmpDept) {
        adminEditEmpDept.innerHTML = depts.map(d => `<option value="${escapeHtml(d.name)}">${escapeHtml(d.name)}</option>`).join('');
      }
      const adminChangeUserDeptSelect = document.getElementById('adminChangeUserDeptSelect');
      if (adminChangeUserDeptSelect) {
        adminChangeUserDeptSelect.innerHTML = depts.map(d => `<option value="${escapeHtml(d.name)}">${escapeHtml(d.name)}</option>`).join('');
      }
      if (adminAttDeptFilter) {
        adminAttDeptFilter.innerHTML = `<option value="ALL">🏢 All Departments (${depts.length})</option>` +
          depts.map(d => `<option value="${escapeHtml(d.name)}">${escapeHtml(d.name)}</option>`).join('');
      }

      if (!adminDepartmentsTableBody) return;

      if (depts.length === 0) {
        adminDepartmentsTableBody.innerHTML = `
          <tr>
            <td colspan="6" style="text-align: center; padding: 24px; color: #64748b;">
              No departments found. Click "+ Add New Department" to create one.
            </td>
          </tr>
        `;
        return;
      }

      adminDepartmentsTableBody.innerHTML = depts.map(d => `
        <tr>
          <td><strong style="font-size: 14px; color: #0f172a;">${escapeHtml(d.name)}</strong></td>
          <td><span class="badge" style="background:#e0f2fe; color:#0369a1; font-weight:700;">${escapeHtml(d.code || 'DEPT')}</span></td>
          <td>${escapeHtml(d.headName || 'Not Assigned')}</td>
          <td><span style="font-size: 12.5px; color: #475569;">${escapeHtml(d.description || '')}</span></td>
          <td>
            <span class="badge" style="background:#f1f5f9; color:#334155; font-weight:700;">
              ${d.totalMembers || 0} Staff
            </span>
          </td>
          <td style="text-align: right;">
            <button class="btn-sm-delete" onclick="deleteDeptFromAdmin(${d.id})">Delete</button>
          </td>
        </tr>
      `).join('');
    } catch (e) {
      if (adminDepartmentsTableBody) {
        adminDepartmentsTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:#ef4444; padding:20px;">Connection error loading departments.</td></tr>`;
      }
    }
  };

  window.deleteDeptFromAdmin = async (id) => {
    if (!confirm('Are you sure you want to delete this department?')) return;
    try {
      const res = await fetch(`${API_BASE}/departments/${id}`, { method: 'DELETE' });
      const json = await res.json();
      if (res.ok) {
        showToast(json.message || 'Department deleted successfully.', 'info');
        loadAdminDepartments();
        updateStats();
      } else {
        showToast(json.message || 'Error deleting department.', 'error');
      }
    } catch (e) {
      showToast('Connection error deleting department.', 'error');
    }
  };

  const openAddDeptModalFunc = () => {
    if (adminAddDeptModalBackdrop) {
      adminAddDeptModalBackdrop.classList.add('open');
      document.body.style.overflow = 'hidden';
    }
  };

  const closeAddDeptModalFunc = () => {
    if (adminAddDeptModalBackdrop) {
      adminAddDeptModalBackdrop.classList.remove('open');
      document.body.style.overflow = '';
    }
  };

  if (openAddDeptModalBtn) openAddDeptModalBtn.addEventListener('click', openAddDeptModalFunc);
  if (closeAdminAddDeptModal) closeAdminAddDeptModal.addEventListener('click', closeAddDeptModalFunc);
  if (cancelAdminAddDeptBtn) cancelAdminAddDeptBtn.addEventListener('click', closeAddDeptModalFunc);
  if (adminAddDeptModalBackdrop) {
    adminAddDeptModalBackdrop.addEventListener('click', (e) => {
      if (e.target === adminAddDeptModalBackdrop) closeAddDeptModalFunc();
    });
  }

  if (adminAddDeptForm) {
    adminAddDeptForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = document.getElementById('adminNewDeptName').value.trim();
      const code = document.getElementById('adminNewDeptCode').value.trim().toUpperCase();
      const headName = document.getElementById('adminNewDeptHead').value.trim();
      const description = document.getElementById('adminNewDeptDesc').value.trim();

      try {
        const res = await fetch(`${API_BASE}/departments`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name, code, headName, description, active: true })
        });
        const json = await res.json();

        if (res.ok && json.success) {
          showToast(`Department "${name}" created successfully!`, 'success');
          closeAddDeptModalFunc();
          adminAddDeptForm.reset();
          loadAdminDepartments();
          updateStats();
        } else {
          showToast(json.message || 'Failed to create department.', 'error');
        }
      } catch (err) {
        showToast('Connection error creating department.', 'error');
      }
    });
  }

  const loadAdminCmsContent = async () => {
    try {
      const res = await fetch(`${API_BASE}/cms/sections`);
      if (!res.ok) return;
      const json = await res.json();
      const sections = json.data || [];

      sections.forEach(sec => {
        if (sec.sectionKey === 'hero') {
          const t = document.getElementById('cmsEditHeroTitle');
          const s = document.getElementById('cmsEditHeroSubtitle');
          if (t) t.value = sec.title || '';
          if (s) s.value = sec.subtitle || '';
        } else if (sec.sectionKey === 'about') {
          const t = document.getElementById('cmsEditAboutTitle');
          const s = document.getElementById('cmsEditAboutSubtitle');
          if (t) t.value = sec.title || '';
          if (s) s.value = sec.subtitle || '';

          if (sec.metaJson) {
            try {
              const meta = JSON.parse(sec.metaJson);
              const c = document.getElementById('cmsEditCompanies');
              const e = document.getElementById('cmsEditEmployees');
              const u = document.getElementById('cmsEditUptime');
              if (c && meta.companies) c.value = meta.companies;
              if (e && meta.employees) e.value = meta.employees;
              if (u && meta.uptime) u.value = meta.uptime;
            } catch (err) {}
          }
        } else if (sec.sectionKey === 'contact') {
          if (sec.metaJson) {
            try {
              const meta = JSON.parse(sec.metaJson);
              const a = document.getElementById('cmsEditAddress');
              const e = document.getElementById('cmsEditEmail');
              const p = document.getElementById('cmsEditPhone');
              if (a && meta.address) a.value = meta.address;
              if (e && meta.email) e.value = meta.email;
              if (p && meta.phone) p.value = meta.phone;
            } catch (err) {}
          }
        }
      });
    } catch (e) {
      console.warn('CMS content load error:', e);
    }
  };

  const saveCmsHeroBtn = document.getElementById('saveCmsHeroBtn');
  if (saveCmsHeroBtn) {
    saveCmsHeroBtn.addEventListener('click', async () => {
      const title = document.getElementById('cmsEditHeroTitle').value.trim();
      const subtitle = document.getElementById('cmsEditHeroSubtitle').value.trim();

      try {
        const res = await fetch(`${API_BASE}/cms/sections/hero`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ title, subtitle })
        });
        const json = await res.json();
        if (res.ok && json.success) {
          showToast('Hero section updated on live website!', 'success');
        } else {
          showToast(json.message || 'Error updating Hero section.', 'error');
        }
      } catch (err) {
        showToast('Connection error updating Hero section.', 'error');
      }
    });
  }

  const saveCmsAboutBtn = document.getElementById('saveCmsAboutBtn');
  if (saveCmsAboutBtn) {
    saveCmsAboutBtn.addEventListener('click', async () => {
      const title = document.getElementById('cmsEditAboutTitle').value.trim();
      const subtitle = document.getElementById('cmsEditAboutSubtitle').value.trim();
      const companies = document.getElementById('cmsEditCompanies').value.trim();
      const employees = document.getElementById('cmsEditEmployees').value.trim();
      const uptime = document.getElementById('cmsEditUptime').value.trim();

      const metaJson = JSON.stringify({ companies, employees, uptime });

      try {
        const res = await fetch(`${API_BASE}/cms/sections/about`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ title, subtitle, metaJson })
        });
        const json = await res.json();
        if (res.ok && json.success) {
          showToast('About section & metrics updated on live website!', 'success');
        } else {
          showToast(json.message || 'Error updating About section.', 'error');
        }
      } catch (err) {
        showToast('Connection error updating About section.', 'error');
      }
    });
  }

  const saveCmsContactBtn = document.getElementById('saveCmsContactBtn');
  if (saveCmsContactBtn) {
    saveCmsContactBtn.addEventListener('click', async () => {
      const address = document.getElementById('cmsEditAddress').value.trim();
      const email = document.getElementById('cmsEditEmail').value.trim();
      const phone = document.getElementById('cmsEditPhone').value.trim();

      const metaJson = JSON.stringify({ address, email, phone });

      try {
        const res = await fetch(`${API_BASE}/cms/sections/contact`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ metaJson })
        });
        const json = await res.json();
        if (res.ok && json.success) {
          showToast('Contact & headquarters info updated on live website!', 'success');
        } else {
          showToast(json.message || 'Error updating Contact section.', 'error');
        }
      } catch (err) {
        showToast('Connection error updating Contact section.', 'error');
      }
    });
  }
  const adminInquiriesTableBody = document.getElementById('adminInquiriesTableBody');
  const refreshInquiriesBtn = document.getElementById('refreshInquiriesBtn');

  const loadAdminInquiries = async () => {
    if (!adminInquiriesTableBody) return;
    try {
      const res = await fetch(`${API_BASE}/contacts`);
      if (!res.ok) throw new Error('Inquiries fetch error');
      const json = await res.json();
      const inqs = json.data || [];

      if (inqs.length === 0) {
        adminInquiriesTableBody.innerHTML = `
          <tr>
            <td colspan="7" style="text-align: center; padding: 36px 20px; color: #64748b;">
              No contact inquiry messages received yet.
            </td>
          </tr>
        `;
        return;
      }

      adminInquiriesTableBody.innerHTML = inqs.map(inq => {
        const isPending = inq.status === 'PENDING';
        const dateStr = inq.createdAt ? inq.createdAt.substring(0, 10) : 'Recent';

        return `
          <tr>
            <td><code style="font-size:12px;">${escapeHtml(dateStr)}</code></td>
            <td><strong>${escapeHtml(inq.name)}</strong></td>
            <td><a href="mailto:${escapeHtml(inq.email)}" style="color:var(--primary);">${escapeHtml(inq.email)}</a></td>
            <td><strong>${escapeHtml(inq.subject)}</strong></td>
            <td><span style="font-size:12.5px; color:#475569;">${escapeHtml(inq.message)}</span></td>
            <td>
              <span class="badge" style="background:${isPending ? '#fee2e2' : '#dcfce7'}; color:${isPending ? '#ef4444' : '#15803d'}; font-weight:700;">
                ${escapeHtml(inq.status)}
              </span>
            </td>
            <td style="text-align: right; white-space: nowrap;">
              ${isPending ? `<button class="btn btn-outline btn-sm" onclick="resolveInquiry(${inq.id})" style="color:#16a34a; border-color:#86efac; margin-right:6px;">Mark Resolved</button>` : ''}
              <button class="btn-sm-delete" onclick="deleteInquiry(${inq.id})">Delete</button>
            </td>
          </tr>
        `;
      }).join('');
    } catch (e) {
      adminInquiriesTableBody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#ef4444; padding:20px;">Connection error loading inquiries.</td></tr>`;
    }
  };

  if (refreshInquiriesBtn) {
    refreshInquiriesBtn.addEventListener('click', () => {
      loadAdminInquiries();
      updateStats();
      showToast('Inquiries refreshed.', 'info');
    });
  }

  window.resolveInquiry = async (id) => {
    try {
      const res = await fetch(`${API_BASE}/contacts/${id}/status?status=RESOLVED`, { method: 'PUT' });
      const json = await res.json();
      if (res.ok) {
        showToast('Inquiry marked as Resolved.', 'success');
        loadAdminInquiries();
        updateStats();
      } else {
        showToast(json.message || 'Error updating status.', 'error');
      }
    } catch (e) {
      showToast('Connection error resolving inquiry.', 'error');
    }
  };

  window.deleteInquiry = async (id) => {
    if (!confirm('Are you sure you want to delete this inquiry message?')) return;
    try {
      const res = await fetch(`${API_BASE}/contacts/${id}`, { method: 'DELETE' });
      const json = await res.json();
      if (res.ok) {
        showToast('Inquiry deleted.', 'info');
        loadAdminInquiries();
        updateStats();
      } else {
        showToast(json.message || 'Error deleting inquiry.', 'error');
      }
    } catch (e) {
      showToast('Connection error deleting inquiry.', 'error');
    }
  };

  // =========================================================
  // DEDICATED INDIVIDUAL USER ATTENDANCE HISTORY INSPECTION
  // =========================================================
  let currentInspectedUserLogs = [];
  let currentInspectedUserEmail = '';
  let currentInspectedUserName = '';

  window.closeAdminUserAttendanceModalFunc = () => {
    const modal = document.getElementById('adminUserAttendanceModalBackdrop');
    if (modal) {
      modal.classList.remove('open');
      modal.style.display = 'none';
      document.body.style.overflow = '';
    }
  };

  const closeAdminUserAttBtn = document.getElementById('closeAdminUserAttendanceModal');
  const closeAdminUserAttFooterBtn = document.getElementById('closeAdminUserAttendanceModalFooter');
  if (closeAdminUserAttBtn) closeAdminUserAttBtn.addEventListener('click', window.closeAdminUserAttendanceModalFunc);
  if (closeAdminUserAttFooterBtn) closeAdminUserAttFooterBtn.addEventListener('click', window.closeAdminUserAttendanceModalFunc);

  const adminUserAttStatusFilter = document.getElementById('adminUserAttStatusFilter');
  const adminUserAttSearchInput = document.getElementById('adminUserAttSearchInput');
  if (adminUserAttStatusFilter) adminUserAttStatusFilter.addEventListener('change', () => renderInspectedUserAttendanceTable());
  if (adminUserAttSearchInput) adminUserAttSearchInput.addEventListener('input', () => renderInspectedUserAttendanceTable());

  window.viewUserAttendanceHistory = async (email, name) => {
    if (!email) return;
    currentInspectedUserEmail = email.trim();
    currentInspectedUserName = name || email.split('@')[0];

    const modal = document.getElementById('adminUserAttendanceModalBackdrop');
    if (!modal) return;

    const targetNameEl = document.getElementById('adminUserAttTargetName');
    const targetEmailEl = document.getElementById('adminUserAttTargetEmail');
    const fullNameDisplayEl = document.getElementById('adminUserAttFullNameDisplay');
    const deptDisplayEl = document.getElementById('adminUserAttDeptDisplay');
    const roleDisplayEl = document.getElementById('adminUserAttRoleDisplay');
    const avatarEl = document.getElementById('adminUserAttAvatarCircle');
    const exportCsvBtn = document.getElementById('adminUserAttExportCsvBtn');
    const tableBody = document.getElementById('adminUserAttendanceTableBody');

    if (targetNameEl) targetNameEl.textContent = currentInspectedUserName;
    if (targetEmailEl) targetEmailEl.textContent = currentInspectedUserEmail;
    if (fullNameDisplayEl) fullNameDisplayEl.textContent = currentInspectedUserName;
    if (avatarEl) avatarEl.textContent = currentInspectedUserName.charAt(0).toUpperCase();

    // Check registeredUsersCache to populate dept & role
    const cachedUser = (registeredUsersCache || []).find(u => u.email && u.email.toLowerCase() === currentInspectedUserEmail.toLowerCase());
    if (cachedUser) {
      if (deptDisplayEl) deptDisplayEl.textContent = cachedUser.department || 'General';
      if (roleDisplayEl) {
        roleDisplayEl.textContent = cachedUser.displayRole || (cachedUser.isAdmin ? 'ADMIN' : 'EMPLOYEE');
      }
    } else {
      if (deptDisplayEl) deptDisplayEl.textContent = 'General';
      if (roleDisplayEl) roleDisplayEl.textContent = 'EMPLOYEE';
    }

    if (exportCsvBtn) {
      exportCsvBtn.href = `/api/reports/attendance/user/csv?email=${encodeURIComponent(currentInspectedUserEmail)}`;
    }

    if (tableBody) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="7" style="text-align: center; padding: 28px; color: #64748b;">
            ⏳ Loading complete shift history for <strong>${escapeHtml(currentInspectedUserName)}</strong>...
          </td>
        </tr>
      `;
    }

    modal.style.display = 'flex';
    modal.classList.add('open');
    document.body.style.overflow = 'hidden';

    try {
      const res = await fetch(`${API_BASE}/attendance/user/${encodeURIComponent(currentInspectedUserEmail)}`);
      if (!res.ok) {
        if (tableBody) {
          tableBody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#ef4444; padding:20px;">Could not load attendance records.</td></tr>`;
        }
        return;
      }
      const json = await res.json();
      currentInspectedUserLogs = json.data || [];
      renderInspectedUserAttendanceTable();
    } catch (e) {
      if (tableBody) {
        tableBody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#ef4444; padding:20px;">Connection error loading attendance logs.</td></tr>`;
      }
    }
  };

  function renderInspectedUserAttendanceTable() {
    const tableBody = document.getElementById('adminUserAttendanceTableBody');
    const statusFilter = document.getElementById('adminUserAttStatusFilter')?.value || 'ALL';
    const searchQuery = (document.getElementById('adminUserAttSearchInput')?.value || '').toLowerCase().trim();

    const kpiTotalEl = document.getElementById('adminUserAttKpiTotal');
    const kpiPresentEl = document.getElementById('adminUserAttKpiPresent');
    const kpiWfhEl = document.getElementById('adminUserAttKpiWfh');
    const kpiLeaveEl = document.getElementById('adminUserAttKpiLeave');
    const kpiHoursEl = document.getElementById('adminUserAttKpiHours');

    let presentCount = 0;
    let wfhCount = 0;
    let leaveCount = 0;
    let totalMinutes = 0;

    currentInspectedUserLogs.forEach(l => {
      const st = (l.status || '').toLowerCase();
      const mode = (l.mode || '').toLowerCase();
      if (st === 'present' || mode === 'office') presentCount++;
      if (st === 'wfh' || mode === 'wfh') wfhCount++;
      if (st === 'on leave' || mode === 'leave') leaveCount++;

      if (l.hours && l.hours !== '0 hrs' && l.hours !== 'In Progress') {
        const hrMatch = l.hours.match(/(\d+)\s*hr/);
        const minMatch = l.hours.match(/(\d+)\s*min/);
        if (hrMatch) totalMinutes += parseInt(hrMatch[1], 10) * 60;
        if (minMatch) totalMinutes += parseInt(minMatch[1], 10);
      }
    });

    const calcHours = Math.floor(totalMinutes / 60);
    const calcMins = totalMinutes % 60;
    let formattedHours = '0 hrs';
    if (calcHours > 0 || calcMins > 0) {
      formattedHours = calcHours > 0 ? `${calcHours}h ${calcMins}m` : `${calcMins}m`;
    } else if (currentInspectedUserLogs.length > 0) {
      formattedHours = `${currentInspectedUserLogs.length * 8}h est.`;
    }

    if (kpiTotalEl) kpiTotalEl.textContent = currentInspectedUserLogs.length;
    if (kpiPresentEl) kpiPresentEl.textContent = presentCount;
    if (kpiWfhEl) kpiWfhEl.textContent = wfhCount;
    if (kpiLeaveEl) kpiLeaveEl.textContent = leaveCount;
    if (kpiHoursEl) kpiHoursEl.textContent = formattedHours;

    let filtered = currentInspectedUserLogs.filter(item => {
      if (statusFilter !== 'ALL' && item.status !== statusFilter) return false;
      if (searchQuery) {
        const text = `${item.date || ''} ${item.mode || ''} ${item.status || ''} ${item.timeIn || ''} ${item.timeOut || ''} ${item.hours || ''}`.toLowerCase();
        if (!text.includes(searchQuery)) return false;
      }
      return true;
    });

    if (!tableBody) return;

    if (filtered.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="7" style="text-align: center; padding: 24px; color: #64748b;">
            No shift records matching your filter.
          </td>
        </tr>
      `;
      return;
    }

    tableBody.innerHTML = filtered.map(item => {
      let statusBg = '#dcfce7';
      let statusColor = '#15803d';
      if (item.status === 'WFH') {
        statusBg = '#dbeafe';
        statusColor = '#1d4ed8';
      } else if (item.status === 'On Leave') {
        statusBg = '#fef3c7';
        statusColor = '#b45309';
      } else if (item.status === 'Absent' || item.status === 'Not Punched In') {
        statusBg = '#fee2e2';
        statusColor = '#b91c1c';
      }

      const isOngoing = (item.timeOut === 'Ongoing Shift' || item.punchedIn);

      return `
        <tr>
          <td><strong style="color: #0f172a;">${escapeHtml(item.date)}</strong></td>
          <td><span style="font-weight: 600; color: #334155;">${escapeHtml(item.mode || 'Office')}</span></td>
          <td><code>${escapeHtml(item.timeIn)}</code></td>
          <td>
            ${isOngoing
              ? `<span style="background: #fef3c7; color: #b45309; font-size: 11px; font-weight: 700; padding: 2px 7px; border-radius: 4px; display: inline-block;">Ongoing Shift</span>`
              : `<code>${escapeHtml(item.timeOut)}</code>`}
          </td>
          <td><span style="color: #475569; font-weight: 600;">${escapeHtml(item.hours)}</span></td>
          <td>
            <span style="display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 11.5px; font-weight: 700; background: ${statusBg}; color: ${statusColor};">
              ${escapeHtml(item.status)}
            </span>
          </td>
          <td style="text-align: right; white-space: nowrap;">
            ${isOngoing ? `
              <button type="button" class="btn btn-sm" onclick="openAdminPunchOutModal(${item.id})" style="background: #ea580c; color: #fff; border: none; padding: 3px 8px; font-size: 11.5px; border-radius: 4px; font-weight: 600; cursor: pointer; box-shadow: 0 1px 2px rgba(0,0,0,0.1);" title="Force punch out for ${escapeHtml(item.date)}">
                🚪 Punch Out
              </button>
            ` : `
              <select class="form-select" style="padding: 2px 6px; font-size: 11px; width: auto; display: inline-block;" onchange="changeAdminHistoryStatus(${item.id}, this.value)">
                <option value="Present" ${item.status === 'Present' ? 'selected' : ''}>Present</option>
                <option value="WFH" ${item.status === 'WFH' ? 'selected' : ''}>WFH</option>
                <option value="On Leave" ${item.status === 'On Leave' ? 'selected' : ''}>On Leave</option>
                <option value="Absent" ${item.status === 'Absent' ? 'selected' : ''}>Absent</option>
              </select>
            `}
          </td>
        </tr>
      `;
    }).join('');
  }

  updateStats();
  renderUsersTable();
  renderAdminEmployeesTable();
  loadAttendanceLogs();
  loadAdminDepartments();
  loadAdminCmsContent();
  loadAdminInquiries();
});

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transition = 'opacity 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}
