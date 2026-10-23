window.switchAuthTab = function(tabName) {
  const tLogin = document.getElementById('tabLoginBtn') || document.getElementById('authTabLogin');
  const tRegister = document.getElementById('tabRegisterBtn') || document.getElementById('authTabRegister');
  const cLogin = document.getElementById('loginTabContent');
  const cRegister = document.getElementById('registerTabContent');

  if (tabName === 'login') {
    if (tLogin) tLogin.classList.add('active');
    if (tRegister) tRegister.classList.remove('active');
    if (cLogin) cLogin.classList.add('active');
    if (cRegister) cRegister.classList.remove('active');
  } else {
    if (tLogin) tLogin.classList.remove('active');
    if (tRegister) tRegister.classList.add('active');
    if (cLogin) cLogin.classList.remove('active');
    if (cRegister) cRegister.classList.add('active');
  }
};

window.openAuthModal = function(tab = 'login') {
  window.switchAuthTab(tab);
  const modal = document.getElementById('authModalBackdrop');
  if (modal) {
    modal.classList.add('open');
    document.body.style.overflow = 'hidden';
  }
};

window.closeAuthModalFunc = function() {
  const modal = document.getElementById('authModalBackdrop');
  if (modal) {
    modal.classList.remove('open');
    document.body.style.overflow = '';
  }
};

window.closeAttendanceTrackerFunc = function() {
  const modal = document.getElementById('attendanceModalBackdrop');
  if (modal) {
    modal.classList.remove('open');
  }
  document.body.style.overflow = '';
};
window.closeAttendanceTracker = window.closeAttendanceTrackerFunc;

window.closeUserRoleModal = function() {
  const modal = document.getElementById('userRoleModalBackdrop');
  if (modal) {
    modal.classList.remove('open');
  }
  document.body.style.overflow = '';
};

document.addEventListener('DOMContentLoaded', () => {

  const getApiBase = () => {
    try {
      if (window.location.protocol === 'file:' || (window.location.port && window.location.port !== '8090')) {
        return 'http://localhost:8090/api';
      }
    } catch (e) {}
    return '/api';
  };
  const API_BASE = getApiBase();

  const getCurrentUser = () => {
    try {
      return JSON.parse(localStorage.getItem('empmanage_session_user') || 'null');
    } catch (e) {
      return null;
    }
  };

  const setCurrentUser = (user) => {
    localStorage.setItem('empmanage_session_user', JSON.stringify(user));
    updateNavbarAuth();
  };

  const removeCurrentUser = () => {
    localStorage.removeItem('empmanage_session_user');
    updateNavbarAuth();
  };

  const openAuthBtn = document.getElementById('openAuthBtn') || document.getElementById('navbarLoginBtn');
  const openRegisterNavBtn = document.getElementById('openRegisterNavBtn') || document.getElementById('navbarRegisterBtn');
  const navbarUserBadge = document.getElementById('navbarUserBadge');
  const navbarAdminLink = document.getElementById('navbarAdminLink');
  const navbarAttendanceBtn = document.getElementById('navbarAttendanceBtn');
  const navbarRoleBtn = document.getElementById('navbarRoleBtn');
  const navbarLogoutBtn = document.getElementById('navbarLogoutBtn');

  const updateNavbarAuth = () => {
    const user = getCurrentUser();
    const btnAuth = document.getElementById('openAuthBtn') || document.getElementById('navbarLoginBtn');
    const btnReg = document.getElementById('openRegisterNavBtn') || document.getElementById('navbarRegisterBtn');
    const badge = document.getElementById('navbarUserBadge');
    const nameEl = document.getElementById('navbarUserName');
    const avatarEl = document.getElementById('navbarUserAvatar');
    const roleEl = document.getElementById('navbarUserRole');
    const adminLink = document.getElementById('navbarAdminLink');
    const adminLoginNavBtn = document.getElementById('openAdminLoginNavBtn');
    const attBtn = document.getElementById('navbarAttendanceBtn');
    const roleBtn = document.getElementById('navbarRoleBtn');
    const logoutBtn = document.getElementById('navbarLogoutBtn');
    const navEmp = document.getElementById('navLinkEmployees');

    if (user && user.email) {
      if (btnAuth) btnAuth.style.display = 'none';
      if (btnReg) btnReg.style.display = 'none';
      if (adminLoginNavBtn) adminLoginNavBtn.style.display = 'none';

      const isAdmin = (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN');
      const displayName = user.fullName || user.email.split('@')[0];

      if (badge) {
        badge.style.display = 'inline-flex';
        if (nameEl) {
          nameEl.textContent = displayName;
          nameEl.title = `${user.fullName || displayName} (${user.email}) - Click to check your role`;
        }
        if (avatarEl) {
          avatarEl.textContent = displayName.charAt(0).toUpperCase();
          avatarEl.style.backgroundColor = isAdmin ? '#dc2626' : 'var(--primary)';
        }
        if (roleEl) {
          roleEl.textContent = user.displayRole ? user.displayRole.toUpperCase() : (isAdmin ? 'ADMIN' : 'STAFF');
          roleEl.style.color = isAdmin ? '#f87171' : '#38bdf8';
        }
      }

      if (adminLink) {
        adminLink.style.display = isAdmin ? 'inline-flex' : 'none';
      }
      if (roleBtn) {
        roleBtn.style.display = 'inline-flex';
        const rolePill = document.getElementById('navbarRolePill');
        if (rolePill) {
          if (isAdmin) {
            rolePill.textContent = 'ADMIN';
            rolePill.style.backgroundColor = '#dc2626';
          } else if (user.role === 'ROLE_HR_MANAGER') {
            rolePill.textContent = 'HR MGR';
            rolePill.style.backgroundColor = '#7c3aed';
          } else if (user.role === 'ROLE_MANAGER') {
            rolePill.textContent = 'MANAGER';
            rolePill.style.backgroundColor = '#4338ca';
          } else if (user.role === 'ROLE_VIEWER') {
            rolePill.textContent = 'VIEWER';
            rolePill.style.backgroundColor = '#d97706';
          } else {
            rolePill.textContent = user.displayRole ? user.displayRole.substring(0, 8).toUpperCase() : 'STAFF';
            rolePill.style.backgroundColor = '#0284c7';
          }
        }
      }
      if (attBtn) attBtn.style.display = 'inline-flex';
      if (logoutBtn) logoutBtn.style.display = 'inline-flex';
      if (navEmp) navEmp.textContent = isAdmin ? 'Employees (Admin)' : 'My Profile';
    } else {
      if (btnAuth) btnAuth.style.display = 'inline-flex';
      if (btnReg) btnReg.style.display = 'inline-flex';
      if (adminLoginNavBtn) adminLoginNavBtn.style.display = 'inline-flex';
      if (badge) badge.style.display = 'none';
      if (adminLink) adminLink.style.display = 'none';
      if (roleBtn) roleBtn.style.display = 'none';
      if (attBtn) attBtn.style.display = 'none';
      if (logoutBtn) logoutBtn.style.display = 'none';
      if (navEmp) navEmp.textContent = 'Employees';
    }

    if (typeof renderDepartmentsGrid === 'function') {
      renderDepartmentsGrid();
    }
    if (typeof renderWorkforceSection === 'function') {
      renderWorkforceSection();
    }
  };

  if (navbarLogoutBtn) {
    navbarLogoutBtn.addEventListener('click', async () => {
      const user = getCurrentUser();
      try {
        await fetch(`${API_BASE}/auth/logout`, { method: 'POST' });
      } catch (e) {}
      removeCurrentUser();
      showToast(`Goodbye, ${user ? user.fullName : 'User'}! You have been signed out.`, 'info');
      window.closeAttendanceTrackerFunc();
    });
  }

  const syncSessionWithServer = async () => {
    try {
      const res = await fetch(`${API_BASE}/auth/me`);
      if (res.ok) {
        const json = await res.json();
        if (json.data && json.data.email) {
          setCurrentUser(json.data);
        }
      } else if (res.status === 401) {
        removeCurrentUser();
      }
    } catch (e) {}
  };
  syncSessionWithServer();

  const navLinks = document.querySelectorAll('.nav-link');
  navLinks.forEach(link => {
    link.addEventListener('click', (e) => {
      const href = link.getAttribute('href');
      if (href && href.startsWith('#')) {
        const target = document.getElementById(href.substring(1));
        if (target) {
          e.preventDefault();
          navLinks.forEach(l => l.classList.remove('active'));
          link.classList.add('active');
          target.scrollIntoView({ behavior: 'smooth' });
        }
      }
    });
  });

  window.navigateToSection = (sectionId) => {
    const el = document.getElementById(sectionId);
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

  const loadCmsContent = async () => {
    try {
      const res = await fetch(`${API_BASE}/cms/sections`);
      if (!res.ok) return;
      const json = await res.json();
      if (!json.success || !json.data) return;

      json.data.forEach(sec => {
        if (sec.sectionKey === 'hero') {
          const heroTitle = document.getElementById('cmsHeroTitle');
          const heroSubtitle = document.getElementById('cmsHeroSubtitle');
          if (heroTitle && sec.title) heroTitle.innerHTML = sec.title.replace(/\n/g, '<br>');
          if (heroSubtitle && sec.subtitle) heroSubtitle.textContent = sec.subtitle;
        } else if (sec.sectionKey === 'about') {
          const aboutTitle = document.getElementById('cmsAboutTitle');
          const aboutDesc = document.getElementById('cmsAboutDesc');
          if (aboutTitle && sec.title) aboutTitle.textContent = sec.title;
          if (aboutDesc && sec.subtitle) aboutDesc.textContent = sec.subtitle;

          if (sec.metaJson) {
            try {
              const meta = JSON.parse(sec.metaJson);
              if (meta.companies) document.getElementById('cmsMetricCompanies').textContent = meta.companies;
              if (meta.employees) document.getElementById('cmsMetricEmployees').textContent = meta.employees;
              if (meta.uptime) document.getElementById('cmsMetricUptime').textContent = meta.uptime;
            } catch (err) {}
          }
        } else if (sec.sectionKey === 'contact') {
          const contactTitle = document.getElementById('cmsContactTitle');
          const contactSubtitle = document.getElementById('cmsContactSubtitle');
          if (contactTitle && sec.title) contactTitle.textContent = sec.title;
          if (contactSubtitle && sec.subtitle) contactSubtitle.textContent = sec.subtitle;

          if (sec.metaJson) {
            try {
              const meta = JSON.parse(sec.metaJson);
              const addr = document.getElementById('cmsContactAddress');
              const email = document.getElementById('cmsContactEmail');
              const phone = document.getElementById('cmsContactPhone');
              if (addr && meta.address) addr.textContent = meta.address;
              if (email && meta.email) {
                email.textContent = meta.email;
                email.href = `mailto:${meta.email}`;
              }
              if (phone && meta.phone) {
                phone.textContent = meta.phone;
                phone.href = `tel:${meta.phone.replace(/[^0-9+]/g, '')}`;
              }
            } catch (err) {}
          }
        }
      });
    } catch (e) {
      console.warn('CMS content fallback to static HTML:', e);
    }
  };

  const departmentsGrid = document.getElementById('departmentsGrid');
  const departmentFilterSelect = document.getElementById('departmentFilterSelect');
  const regDepartment = document.getElementById('regDepartment');
  const newEmpDept = document.getElementById('newEmpDept');

  let cachedDepartments = [];

  const getDeptMetadata = (dept) => {
    const name = (dept.name || '').toLowerCase();
    const code = (dept.code || '').toUpperCase();

    if (code === 'ENG' || name.includes('engineer') || name.includes('tech') || name.includes('it')) {
      return {
        themeClass: 'dept-theme-eng',
        code: dept.code || 'ENG-01',
        iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><polyline points="16 18 22 12 16 6"></polyline><polyline points="8 6 2 12 8 18"></polyline></svg>`,
        tags: ['Java / Spring Boot', 'Cloud Architecture', 'REST APIs', 'DevOps & CI/CD'],
        leadRole: 'Lead Software Architect'
      };
    }
    if (code === 'HR' || name.includes('human') || name.includes('talent') || name.includes('people')) {
      return {
        themeClass: 'dept-theme-hr',
        code: dept.code || 'HR-01',
        iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>`,
        tags: ['Talent Acquisition', 'Attendance & Shifts', 'Employee Welfare', 'Compliance'],
        leadRole: 'VP of People Operations'
      };
    }
    if (code === 'SLS' || name.includes('sales') || name.includes('business') || name.includes('commercial')) {
      return {
        themeClass: 'dept-theme-sls',
        code: dept.code || 'SLS-01',
        iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><line x1="12" y1="1" x2="12" y2="23"></line><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path></svg>`,
        tags: ['B2B Enterprise', 'Client Partnerships', 'Revenue Growth', 'Client Success'],
        leadRole: 'Chief Commercial Officer'
      };
    }
    if (code === 'FIN' || name.includes('finance') || name.includes('account')) {
      return {
        themeClass: 'dept-theme-fin',
        code: dept.code || 'FIN-01',
        iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><rect x="2" y="4" width="20" height="16" rx="2"></rect><line x1="2" y1="10" x2="22" y2="10"></line></svg>`,
        tags: ['Corporate Audits', 'Payroll Automation', 'Financial Planning', 'Taxation'],
        leadRole: 'Head of Financial Planning'
      };
    }
    if (code === 'MKT' || name.includes('market') || name.includes('comm')) {
      return {
        themeClass: 'dept-theme-mkt',
        code: dept.code || 'MKT-01',
        iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>`,
        tags: ['Brand Strategy', 'Performance Growth', 'Social Engagement', 'PR & Media'],
        leadRole: 'Chief Brand Officer'
      };
    }
    if (code === 'OPS' || name.includes('operat') || name.includes('qa') || name.includes('quality')) {
      return {
        themeClass: 'dept-theme-ops',
        code: dept.code || 'OPS-01',
        iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline></svg>`,
        tags: ['Quality Assurance', 'SLA Governance', 'Process Automation', 'Delivery QA'],
        leadRole: 'Director of Quality Assurance'
      };
    }

    return {
      themeClass: 'dept-theme-eng',
      code: dept.code || 'UNIT-01',
      iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><rect x="4" y="2" width="16" height="20" rx="2" ry="2"></rect><path d="M9 22v-4h6v4"></path></svg>`,
      tags: ['Operational Division', 'Business Services'],
      leadRole: 'Division Director'
    };
  };

  const renderDepartmentsGrid = () => {
    if (!departmentsGrid || cachedDepartments.length === 0) return;
    const user = getCurrentUser();
    const isAdmin = user && (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN' || user.admin === true);

    departmentsGrid.innerHTML = cachedDepartments.map((d) => {
      const meta = getDeptMetadata(d);
      const leadName = d.headName || meta.leadRole;
      return `
        <div class="department-card ${meta.themeClass}">
          <div class="dept-header">
            <div class="dept-icon-box">
              ${meta.iconSvg}
            </div>
            <div class="dept-badge-group">
              <span class="badge badge-code">${escapeHtml(meta.code)}</span>
              <span class="badge badge-active">${d.active ? 'Operational' : 'Archived'}</span>
            </div>
          </div>
          <h3 class="dept-name">${escapeHtml(d.name)}</h3>
          <p class="dept-desc">${escapeHtml(d.description || 'Core organizational business division.')}</p>
          
          <div class="dept-info-strip">
            <div class="dept-info-item">
              <div class="dept-info-icon-badge">👔</div>
              <div>
                <span class="dept-info-label">Division Leadership</span>
                <span class="dept-info-value">${escapeHtml(leadName)}</span>
              </div>
            </div>
          </div>

          <div class="dept-tags">
            ${meta.tags.map(t => `<span class="dept-tag">${escapeHtml(t)}</span>`).join('')}
          </div>

          <div class="dept-footer-bar">
            <span class="dept-status-indicator">
              <span class="dot-active"></span> Active Unit
            </span>
            ${isAdmin ? `
              <button type="button" class="btn btn-primary btn-sm" onclick="filterByDeptAndScroll('${escapeHtml(d.name)}')">
                View Staff Roster (${d.totalMembers || 0}) &rarr;
              </button>
            ` : `
              <span class="dept-unit-badge">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                Core Division
              </span>
            `}
          </div>
        </div>
      `;
    }).join('');
  };

  const loadDepartments = async () => {
    try {
      const res = await fetch(`${API_BASE}/departments`);
      if (!res.ok) return;
      const json = await res.json();
      if (!json.success || !json.data) return;

      cachedDepartments = json.data;
      const depts = cachedDepartments;

      if (departmentFilterSelect) {
        departmentFilterSelect.innerHTML = `<option value="ALL">All Departments (${depts.length})</option>` +
          depts.map(d => `<option value="${escapeHtml(d.name)}">${escapeHtml(d.name)}</option>`).join('');
      }

      if (regDepartment) {
        regDepartment.innerHTML = depts.map(d => `<option value="${escapeHtml(d.name)}">${escapeHtml(d.name)}</option>`).join('');
      }

      if (newEmpDept) {
        newEmpDept.innerHTML = depts.map(d => `<option value="${escapeHtml(d.name)}">${escapeHtml(d.name)}</option>`).join('');
      }

      renderDepartmentsGrid();
    } catch (e) {
      console.warn('Departments fetch failed:', e);
    }
  };

  // Department Roster Modal controls (Strictly Admin Only)
  const deptRosterModalBackdrop = document.getElementById('deptRosterModalBackdrop');
  const deptRosterModalTitle = document.getElementById('deptRosterModalTitle');
  const deptRosterModalSubtitle = document.getElementById('deptRosterModalSubtitle');
  const deptRosterTableBody = document.getElementById('deptRosterTableBody');
  const closeDeptRosterModal = document.getElementById('closeDeptRosterModal');
  const closeDeptRosterModalBtn = document.getElementById('closeDeptRosterModalBtn');

  const closeDeptRosterModalFn = () => {
    if (deptRosterModalBackdrop) deptRosterModalBackdrop.classList.remove('active');
  };

  if (closeDeptRosterModal) closeDeptRosterModal.addEventListener('click', closeDeptRosterModalFn);
  if (closeDeptRosterModalBtn) closeDeptRosterModalBtn.addEventListener('click', closeDeptRosterModalFn);
  if (deptRosterModalBackdrop) {
    deptRosterModalBackdrop.addEventListener('click', (e) => {
      if (e.target === deptRosterModalBackdrop) closeDeptRosterModalFn();
    });
  }

  window.openDeptRosterModal = async (deptName) => {
    if (!deptRosterModalBackdrop || !deptRosterTableBody) return;

    if (deptRosterModalTitle) {
      deptRosterModalTitle.innerHTML = `<span>👥 ${escapeHtml(deptName)} - Personnel Roster</span>`;
    }
    if (deptRosterModalSubtitle) {
      deptRosterModalSubtitle.textContent = `Fetching personnel allocated to ${deptName}...`;
    }

    deptRosterTableBody.innerHTML = `
      <tr>
        <td colspan="4" style="text-align: center; padding: 24px; color: #64748b;">
          Loading employees...
        </td>
      </tr>
    `;

    deptRosterModalBackdrop.classList.add('active');

    try {
      const res = await fetch(`${API_BASE}/employees?dept=${encodeURIComponent(deptName)}`);
      if (!res.ok) throw new Error('Failed to fetch department employees');
      const json = await res.json();
      const rawData = json.data;
      const emps = Array.isArray(rawData) ? rawData : (rawData?.content || rawData?.employees || []);

      if (deptRosterModalSubtitle) {
        deptRosterModalSubtitle.innerHTML = `Verified Administrator view &bull; <strong>${emps.length} personnel</strong> assigned to this department.`;
      }

      if (emps.length === 0) {
        deptRosterTableBody.innerHTML = `
          <tr>
            <td colspan="4" style="text-align: center; padding: 32px 16px; color: #64748b;">
              No employee personnel currently assigned to ${escapeHtml(deptName)}.
            </td>
          </tr>
        `;
        return;
      }

      deptRosterTableBody.innerHTML = emps.map(emp => `
        <tr>
          <td>
            <div style="display:flex; align-items:center; gap:8px;">
              <div style="width:30px; height:30px; border-radius:50%; background:#eff6ff; color:#1d4ed8; font-weight:700; display:flex; align-items:center; justify-content:center; font-size:12px; flex-shrink:0;">
                ${escapeHtml(emp.name ? emp.name.charAt(0).toUpperCase() : 'E')}
              </div>
              <div>
                <strong style="color:#0f172a; font-size:13px;">${escapeHtml(emp.name)}</strong>
                <div style="font-size:11px; color:#64748b; font-family:monospace;">${escapeHtml(emp.employeeCode || '')}</div>
              </div>
            </div>
          </td>
          <td><span style="font-size:13px; color:#334155;">${escapeHtml(emp.title || 'Staff')}</span></td>
          <td><a href="mailto:${escapeHtml(emp.email)}" style="font-size:12.5px; color:var(--primary); font-family:monospace;">${escapeHtml(emp.email)}</a></td>
          <td><span class="status-badge active" style="font-size:11.5px;">${escapeHtml(emp.status || 'Active')}</span></td>
        </tr>
      `).join('');

      // Also sync Employee Directory on page
      if (departmentFilterSelect) {
        departmentFilterSelect.value = deptName;
        loadEmployees('', deptName);
      }
    } catch (e) {
      deptRosterTableBody.innerHTML = `
        <tr>
          <td colspan="4" style="text-align:center; padding:20px; color:#ef4444;">
            Error loading department employees roster.
          </td>
        </tr>
      `;
    }
  };

  window.filterByDeptAndScroll = async (deptName) => {
    const user = getCurrentUser();
    const isAdmin = user && (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN' || user.admin === true);

    if (!isAdmin) {
      if (!user) {
        showToast('🔒 Admin Login Required: Only Administrators can view which employees belong to this department.', 'warning');
        window.openAuthModal('login');
        const roleAdminRadio = document.getElementById('loginRoleAdmin');
        if (roleAdminRadio) roleAdminRadio.checked = true;
      } else {
        showToast('🔒 Access Restricted: Only Administrators are permitted to view department employee rosters.', 'warning');
      }
      return;
    }

    openDeptRosterModal(deptName);
  };

  const employeeTableBody = document.getElementById('employeeTableBody');
  const employeeSearchInput = document.getElementById('employeeSearchInput');

  const loadEmployeePersonalProfile = async (user) => {
    try {
      let emp = null;
      const res = await fetch(`${API_BASE}/employees/me?email=${encodeURIComponent(user.email)}`);
      if (res.ok) {
        const json = await res.json();
        emp = json.data;
      }

      const empName = emp?.name || user.fullName || user.email.split('@')[0];
      const empCode = emp?.employeeCode || 'EMP-001';
      const empTitle = emp?.title || user.title || 'Staff Member';
      const empDept = emp?.departmentName || (emp?.department && emp.department.name) || user.department || 'General';
      const empEmail = emp?.email || user.email;
      const empStatus = emp?.status || 'Active';

      const avatarEl = document.getElementById('empCardAvatar');
      const nameEl = document.getElementById('empCardName');
      const codeEl = document.getElementById('empCardCode');
      const statusEl = document.getElementById('empCardStatus');
      const titleEl = document.getElementById('empCardTitle');
      const deptEl = document.getElementById('empCardDept');
      const emailEl = document.getElementById('empCardEmail');
      const roleEl = document.getElementById('empCardRole');

      if (avatarEl) avatarEl.textContent = empName.charAt(0).toUpperCase();
      if (nameEl) nameEl.textContent = empName;
      if (codeEl) codeEl.textContent = empCode;
      if (statusEl) {
        statusEl.textContent = empStatus;
        statusEl.className = 'status-badge ' + (empStatus.toLowerCase() === 'active' ? 'active' : 'inactive');
      }
      if (titleEl) titleEl.textContent = empTitle;
      if (deptEl) deptEl.textContent = empDept;
      if (emailEl) emailEl.textContent = empEmail;
      const isAdmin = (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN');
      if (roleEl) roleEl.textContent = user.displayRole || (isAdmin ? 'Administrator (Executive)' : 'Standard Employee (Staff)');

      const roleTierBadge = document.getElementById('empCardRoleTierBadge');
      if (roleTierBadge) {
        if (isAdmin) {
          roleTierBadge.textContent = 'Tier 4 • Super Admin';
          roleTierBadge.style.backgroundColor = '#fee2e2';
          roleTierBadge.style.color = '#dc2626';
        } else if (user.role === 'ROLE_HR_MANAGER') {
          roleTierBadge.textContent = 'Tier 3 • HR Executive';
          roleTierBadge.style.backgroundColor = '#ede9fe';
          roleTierBadge.style.color = '#7c3aed';
        } else if (user.role === 'ROLE_MANAGER') {
          roleTierBadge.textContent = 'Tier 2 • Department Lead';
          roleTierBadge.style.backgroundColor = '#e0e7ff';
          roleTierBadge.style.color = '#4338ca';
        } else if (user.role === 'ROLE_VIEWER') {
          roleTierBadge.textContent = 'Tier 0 • Audit Observer';
          roleTierBadge.style.backgroundColor = '#fef3c7';
          roleTierBadge.style.color = '#b45309';
        } else {
          roleTierBadge.textContent = 'Tier 1 • Staff Member';
          roleTierBadge.style.backgroundColor = '#e0f2fe';
          roleTierBadge.style.color = '#0284c7';
        }
      }

      const policyNameEl = document.getElementById('empCardPolicyName');
      if (policyNameEl) {
        policyNameEl.textContent = `${user.displayRole || 'Standard Employee'} (${user.role || 'ROLE_USER'})`;
      }
    } catch (e) {
      console.warn('Error loading employee personal profile:', e);
    }
  };

  const renderWorkforceSection = async () => {
    const user = getCurrentUser();
    const guestView = document.getElementById('guestRestrictedView');
    const personalView = document.getElementById('employeePersonalView');
    const adminView = document.getElementById('adminDirectoryView');

    const eyebrow = document.getElementById('workforceEyebrow');
    const title = document.getElementById('workforceTitle');
    const subtitle = document.getElementById('workforceSubtitle');
    const featTitle = document.getElementById('featureCardEmpTitle');
    const featDesc = document.getElementById('featureCardEmpDesc');
    const featLink = document.getElementById('featureCardEmpLinkText');

    if (!guestView || !personalView || !adminView) return;

    if (!user || !user.email) {
      // 1. Guest state
      guestView.style.display = 'block';
      personalView.style.display = 'none';
      adminView.style.display = 'none';

      if (eyebrow) eyebrow.textContent = 'WORKFORCE DIRECTORY';
      if (title) title.textContent = 'Employee Directory';
      if (subtitle) subtitle.textContent = 'Company workforce records are protected by enterprise access policies.';
      if (featTitle) featTitle.textContent = 'Employee Management';
      if (featDesc) featDesc.textContent = 'Add, update, view and manage employee details easily.';
      if (featLink) featLink.textContent = 'Go to Employees';
      return;
    }

    const isAdmin = (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN' || user.displayRole === 'ADMIN');

    if (isAdmin) {
      // 2. Administrator state
      guestView.style.display = 'none';
      personalView.style.display = 'none';
      adminView.style.display = 'block';

      if (eyebrow) eyebrow.textContent = 'WORKFORCE DIRECTORY (ADMIN CONSOLE)';
      if (title) title.textContent = 'Enterprise Employee Directory';
      if (subtitle) subtitle.textContent = 'Review company personnel. Complete employee CRUD actions are centralized in the Admin Portal.';
      if (featTitle) featTitle.textContent = 'Workforce Administration';
      if (featDesc) featDesc.textContent = 'Manage all company employee records, roles, and shifts.';
      if (featLink) featLink.textContent = 'Manage Employees';

      loadEmployees(
        employeeSearchInput ? employeeSearchInput.value : '',
        departmentFilterSelect ? departmentFilterSelect.value : 'ALL'
      );
    } else {
      // 3. Regular Employee state
      guestView.style.display = 'none';
      personalView.style.display = 'block';
      adminView.style.display = 'none';

      if (eyebrow) eyebrow.textContent = 'MY WORKFORCE PROFILE';
      if (title) title.textContent = 'My Employee Details';
      if (subtitle) subtitle.textContent = 'Your verified employment credentials, role assignment, and shift shortcuts.';
      if (featTitle) featTitle.textContent = 'My Employee Profile';
      if (featDesc) featDesc.textContent = 'View your verified employment record and shift status.';
      if (featLink) featLink.textContent = 'View My Profile';

      await loadEmployeePersonalProfile(user);
    }
  };

  const loadEmployees = async (query = '', dept = 'ALL') => {
    if (!employeeTableBody) return;
    try {
      const user = getCurrentUser();
      let url = `${API_BASE}/employees?`;
      if (query && query.trim()) url += `query=${encodeURIComponent(query.trim())}&`;
      if (dept && dept !== 'ALL') url += `dept=${encodeURIComponent(dept)}&`;
      if (user && user.email) url += `userEmail=${encodeURIComponent(user.email)}&`;

      const res = await fetch(url);
      if (!res.ok) throw new Error('Failed to fetch employees');
      const json = await res.json();
      const rawData = json.data;
      const employees = Array.isArray(rawData) ? rawData : (rawData?.content || rawData?.employees || []);

      if (employees.length === 0) {
        employeeTableBody.innerHTML = `
          <tr>
            <td colspan="5" style="text-align: center; padding: 36px 20px; color: var(--text-muted);">
              No employee records found matching the specified criteria.
            </td>
          </tr>
        `;
        return;
      }

      employeeTableBody.innerHTML = employees.map(emp => {
        const deptName = emp.departmentName || (emp.department && emp.department.name) || (typeof emp.department === 'string' ? emp.department : '') || 'General';
        return `
        <tr>
          <td>
            <div style="display:flex; align-items:center; gap:10px;">
              <div style="width:34px; height:34px; border-radius:50%; background:#eff6ff; color:#1d4ed8; font-weight:700; display:flex; align-items:center; justify-content:center; font-size:13px;">
                ${escapeHtml(emp.name ? emp.name.charAt(0).toUpperCase() : 'E')}
              </div>
              <div>
                <strong>${escapeHtml(emp.name)}</strong>
                <div style="font-size:11px; color:#64748b; font-family:monospace;">${escapeHtml(emp.employeeCode || '')}</div>
              </div>
            </div>
          </td>
          <td>${escapeHtml(emp.title || 'Staff')}</td>
          <td><span style="font-size:13px; font-weight:600; color:#334155;">${escapeHtml(deptName)}</span></td>
          <td><a href="mailto:${escapeHtml(emp.email)}" style="color:var(--primary);">${escapeHtml(emp.email)}</a></td>
          <td><span class="status-badge active">${escapeHtml(emp.status || 'Active')}</span></td>
        </tr>
      `}).join('');
    } catch (e) {
      employeeTableBody.innerHTML = `
        <tr>
          <td colspan="5" style="text-align: center; padding: 24px; color: #ef4444;">
            Error connecting to database. Please make sure the Spring Boot server is running on port 8090.
          </td>
        </tr>
      `;
    }
  };

  if (employeeSearchInput) {
    employeeSearchInput.addEventListener('input', () => {
      loadEmployees(employeeSearchInput.value, departmentFilterSelect ? departmentFilterSelect.value : 'ALL');
    });
  }

  if (departmentFilterSelect) {
    departmentFilterSelect.addEventListener('change', () => {
      loadEmployees(employeeSearchInput ? employeeSearchInput.value : '', departmentFilterSelect.value);
    });
  }

  const contactForm = document.getElementById('contactForm');
  if (contactForm) {
    contactForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = document.getElementById('contactName').value.trim();
      const email = document.getElementById('contactEmail').value.trim();
      const subject = document.getElementById('contactSubject').value.trim();
      const message = document.getElementById('contactMessage').value.trim();

      try {
        const res = await fetch(`${API_BASE}/contacts`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name, email, subject, message })
        });
        const json = await res.json();

        if (res.ok && json.success) {
          contactForm.reset();
          showToast(`Thank you, ${name}! Your inquiry has been submitted to support.`, 'success');
        } else {
          showToast(json.message || 'Failed to submit inquiry.', 'error');
        }
      } catch (err) {
        showToast('Connection error submitting inquiry.', 'error');
      }
    });
  }

  const authModalBackdrop = document.getElementById('authModalBackdrop');
  const closeAuthModal = document.getElementById('closeAuthModal');
  const tabLoginBtn = document.getElementById('tabLoginBtn') || document.getElementById('authTabLogin');
  const tabRegisterBtn = document.getElementById('tabRegisterBtn') || document.getElementById('authTabRegister');
  const loginTabContent = document.getElementById('loginTabContent');
  const registerTabContent = document.getElementById('registerTabContent');
  const loginForm = document.getElementById('loginForm');
  const registerForm = document.getElementById('registerForm');

  window.openAuthModal = (tab = 'login') => {
    window.switchAuthTab(tab);
    const modal = document.getElementById('authModalBackdrop');
    if (modal) {
      modal.classList.add('open');
      document.body.style.overflow = 'hidden';
      if (tab === 'admin') {
        const rAdmin = document.getElementById('loginRoleAdmin');
        if (rAdmin) rAdmin.checked = true;
        const emailInput = document.getElementById('loginEmailInput');
        if (emailInput && !emailInput.value) emailInput.value = 'admin@empmanage.com';
        const pwdInput = document.getElementById('loginPasswordInput');
        if (pwdInput) setTimeout(() => pwdInput.focus(), 100);
      }
    }
  };

  window.closeAuthModalFunc = () => {
    const modal = document.getElementById('authModalBackdrop');
    if (modal) {
      modal.classList.remove('open');
      document.body.style.overflow = '';
    }
  };

  window.switchAuthTab = (tabName) => {
    const tLogin = document.getElementById('tabLoginBtn') || document.getElementById('authTabLogin');
    const tRegister = document.getElementById('tabRegisterBtn') || document.getElementById('authTabRegister');
    const cLogin = document.getElementById('loginTabContent');
    const cRegister = document.getElementById('registerTabContent');

    if (tabName === 'login' || tabName === 'admin') {
      if (tLogin) tLogin.classList.add('active');
      if (tRegister) tRegister.classList.remove('active');
      if (cLogin) cLogin.classList.add('active');
      if (cRegister) cRegister.classList.remove('active');

      if (tabName === 'admin') {
        const rAdmin = document.getElementById('loginRoleAdmin');
        if (rAdmin) rAdmin.checked = true;
        const emailInput = document.getElementById('loginEmailInput');
        if (emailInput && !emailInput.value) emailInput.value = 'admin@empmanage.com';
      }
    } else {
      if (tLogin) tLogin.classList.remove('active');
      if (tRegister) tRegister.classList.add('active');
      if (cLogin) cLogin.classList.remove('active');
      if (cRegister) cRegister.classList.add('active');
    }
  };

  window.openLoginDirect = () => {
    const user = getCurrentUser();
    if (user && (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN' || (user.permissions && user.permissions.includes('ROLE_MANAGE')))) {
      window.location.href = 'admin.html';
      return;
    }
    const featModal = document.getElementById('featureModalBackdrop');
    if (featModal) featModal.classList.remove('open');
    window.openAuthModal('admin');
  };

  const btnOpenAuth = document.getElementById('openAuthBtn') || document.getElementById('navbarLoginBtn');
  if (btnOpenAuth) {
    btnOpenAuth.addEventListener('click', (e) => {
      e.preventDefault();
      window.openAuthModal('login');
    });
  }

  const btnOpenReg = document.getElementById('openRegisterNavBtn') || document.getElementById('navbarRegisterBtn');
  if (btnOpenReg) {
    btnOpenReg.addEventListener('click', (e) => {
      e.preventDefault();
      window.openAuthModal('register');
    });
  }

  if (tabLoginBtn) tabLoginBtn.addEventListener('click', () => window.switchAuthTab('login'));
  if (tabRegisterBtn) tabRegisterBtn.addEventListener('click', () => window.switchAuthTab('register'));
  if (closeAuthModal) closeAuthModal.addEventListener('click', window.closeAuthModalFunc);
  if (authModalBackdrop) {
    authModalBackdrop.addEventListener('click', (e) => {
      if (e.target === authModalBackdrop) window.closeAuthModalFunc();
    });
  }

  const fillDemoAdminBtn = document.getElementById('fillDemoAdminBtn');
  const fillDemoEmpBtn = document.getElementById('fillDemoEmpBtn');

  if (fillDemoAdminBtn) {
    fillDemoAdminBtn.addEventListener('click', () => {
      document.getElementById('loginEmailInput').value = 'admin@empmanage.com';
      document.getElementById('loginPasswordInput').value = 'admin123';
      const r = document.getElementById('loginRoleAdmin');
      if (r) r.checked = true;
      showToast('Admin credentials filled! Click "Sign In" to proceed.', 'info');
    });
  }

  if (fillDemoEmpBtn) {
    fillDemoEmpBtn.addEventListener('click', () => {
      document.getElementById('loginEmailInput').value = 'employee@empmanage.com';
      document.getElementById('loginPasswordInput').value = 'emp123';
      const r = document.getElementById('loginRoleEmployee');
      if (r) r.checked = true;
      showToast('Employee credentials filled! Click "Sign In" to proceed.', 'info');
    });
  }

  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      let email = document.getElementById('loginEmailInput').value.trim();
      const password = document.getElementById('loginPasswordInput').value;
      const role = document.querySelector('input[name="loginRole"]:checked')?.value || 'EMPLOYEE';

      // Shorthand aliases support on client
      if (email.toLowerCase() === 'admin') email = 'admin@empmanage.com';
      if (email.toLowerCase() === 'employee') email = 'employee@empmanage.com';

      try {
        const res = await fetch(`${API_BASE}/auth/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password, role })
        });
        const json = await res.json();

        if (res.ok && json.success && json.data) {
          const user = json.data;
          setCurrentUser(user);
          window.closeAuthModalFunc();

          const isAdmin = (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN' || (user.role && user.role.toUpperCase().includes('ADMIN')) || role === 'ADMIN' || (user.permissions && user.permissions.includes('ROLE_MANAGE')));

          if (isAdmin) {
            showToast(`Welcome Administrator, ${user.fullName || 'Admin'}! Opening Admin Portal...`, 'success');
            // Direct immediate redirect to admin.html
            setTimeout(() => {
              window.location.href = 'admin.html';
            }, 300);
          } else {
            showToast(`Welcome back, ${user.fullName} (${user.displayRole || user.role})!`, 'success');
            updateNavbarAuth();
            if (typeof renderWorkforceSection === 'function') renderWorkforceSection();
            if (typeof renderDepartmentsGrid === 'function') renderDepartmentsGrid();

            // For employee, open personalized attendance shift clock
            setTimeout(() => {
              window.openAttendanceTracker();
            }, 400);
          }
        } else {
          showToast(json.message || 'Login failed. Please verify credentials.', 'error');
        }
      } catch (err) {
        showToast('Connection error connecting to authentication server.', 'error');
      }
    });
  }

  // Department-to-Role Dynamic Mapping for Registration
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

  const updateRegTitleOptions = () => {
    const regDept = document.getElementById('regDepartment');
    const regTitle = document.getElementById('regTitle');
    if (!regDept || !regTitle) return;
    const roles = getRolesForDepartment(regDept.value);
    regTitle.innerHTML = roles.map(r => `<option value="${escapeHtml(r)}">${escapeHtml(r)}</option>`).join('');
  };

  const regDeptSelect = document.getElementById('regDepartment');
  if (regDeptSelect) {
    regDeptSelect.addEventListener('change', updateRegTitleOptions);
    updateRegTitleOptions();
  }

  if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const role = document.querySelector('input[name="registerRole"]:checked')?.value || 'EMPLOYEE';
      const fullName = document.getElementById('regFullName').value.trim();
      const email = document.getElementById('regEmail').value.trim().toLowerCase();
      const department = document.getElementById('regDepartment').value;
      const password = document.getElementById('regPassword').value;
      const confirmPassword = document.getElementById('regConfirmPassword').value;

      if (password !== confirmPassword) {
        showToast('Passwords do not match! Please verify.', 'error');
        return;
      }
      if (password.length < 6) {
        showToast('Password must be at least 6 characters long.', 'error');
        return;
      }

      const title = document.getElementById('regTitle')?.value?.trim();
      const phone = document.getElementById('regPhone')?.value?.trim();

      try {
        const res = await fetch(`${API_BASE}/auth/register`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ fullName, email, password, role, department, title, phone })
        });
        const json = await res.json();

        if (res.ok && json.success) {
          showToast(`Account registered for ${fullName}! Please sign in.`, 'success');
          registerForm.reset();
          loadEmployees();
          loadDepartments();
          window.switchAuthTab('login');
          document.getElementById('loginEmailInput').value = email;
          const roleRadios = document.querySelectorAll('input[name="loginRole"]');
          roleRadios.forEach(r => { if (r.value === role) r.checked = true; });
        } else {
          showToast(json.message || 'Registration failed.', 'error');
        }
      } catch (err) {
        showToast('Connection error during registration.', 'error');
      }
    });
  }

  const attendanceModalBackdrop = document.getElementById('attendanceModalBackdrop');
  const closeAttendanceModal = document.getElementById('closeAttendanceModal');
  const liveClockDisplay = document.getElementById('liveClockDisplay');
  const liveDateDisplay = document.getElementById('liveDateDisplay');
  const punchClockBtn = document.getElementById('punchClockBtn');
  const punchClockIcon = document.getElementById('punchClockIcon');
  const punchClockText = document.getElementById('punchClockText');
  const userPunchBadge = document.getElementById('userPunchBadge');
  const myAttendanceHistoryTableBody = document.getElementById('myAttendanceHistoryTableBody');

  let clockInterval = null;

  const updateClockUI = () => {
    const now = new Date();
    if (liveClockDisplay) {
      liveClockDisplay.textContent = now.toLocaleTimeString('en-US', { hour12: true });
    }
    if (liveDateDisplay) {
      liveDateDisplay.textContent = `Today: ${now.toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'short', day: 'numeric' })}`;
    }
  };

  const loadMyAttendance = async (user) => {
    if (!user || !user.email) return;
    try {
      const res = await fetch(`${API_BASE}/attendance/my?email=${encodeURIComponent(user.email)}`);
      if (!res.ok) return;
      const json = await res.json();
      const logs = json.data || [];

      const now = new Date();
      const todayKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
      const todayLog = logs.find(l => l.dateKey === todayKey);

      if (todayLog && todayLog.punchedIn) {
        if (userPunchBadge) {
          userPunchBadge.textContent = `Punched In (${todayLog.timeIn})`;
          userPunchBadge.style.backgroundColor = '#dcfce7';
          userPunchBadge.style.color = '#15803d';
        }
        if (punchClockIcon) punchClockIcon.textContent = '🔴';
        if (punchClockText) punchClockText.textContent = 'Punch Out (Clock Out)';
        if (punchClockBtn) {
          punchClockBtn.style.backgroundColor = '#dc2626';
          punchClockBtn.style.borderColor = '#dc2626';
        }
      } else {
        if (userPunchBadge) {
          if (todayLog && todayLog.timeOut && todayLog.timeOut !== '--:--' && todayLog.timeOut !== 'Ongoing Shift') {
            userPunchBadge.textContent = `Punched Out (${todayLog.timeOut})`;
            userPunchBadge.style.backgroundColor = '#f1f5f9';
            userPunchBadge.style.color = '#475569';
          } else {
            userPunchBadge.textContent = 'Not Punched In';
            userPunchBadge.style.backgroundColor = '#e2e8f0';
            userPunchBadge.style.color = '#475569';
          }
        }
        if (punchClockIcon) punchClockIcon.textContent = '🟢';
        if (punchClockText) punchClockText.textContent = 'Punch In (Clock In)';
        if (punchClockBtn) {
          punchClockBtn.style.backgroundColor = '';
          punchClockBtn.style.borderColor = '';
        }
      }

      if (myAttendanceHistoryTableBody) {
        if (logs.length === 0) {
          myAttendanceHistoryTableBody.innerHTML = `
            <tr>
              <td colspan="6" style="text-align: center; padding: 24px; color: var(--text-muted);">
                No shift history recorded yet. Use the punch clock above to start your shift.
              </td>
            </tr>
          `;
          return;
        }

        myAttendanceHistoryTableBody.innerHTML = logs.map(item => {
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

          return `
            <tr>
              <td><strong style="color:var(--text-main);">${escapeHtml(item.date)}</strong></td>
              <td><span style="font-weight:600;">${escapeHtml(item.mode || 'Office')}</span></td>
              <td><code>${escapeHtml(item.timeIn)}</code></td>
              <td><code>${escapeHtml(item.timeOut)}</code></td>
              <td><span style="color:#475569;">${escapeHtml(item.hours)}</span></td>
              <td>
                <span style="display:inline-block; padding:3px 10px; border-radius:12px; font-size:11.5px; font-weight:700; background:${statusBg}; color:${statusColor};">
                  ${escapeHtml(item.status)}
                </span>
              </td>
            </tr>
          `;
        }).join('');
      }

    } catch (e) {
      console.warn('Attendance load error:', e);
    }
  };

  if (punchClockBtn) {
    punchClockBtn.addEventListener('click', async () => {
      const user = getCurrentUser();
      if (!user) {
        showToast('Please sign in to record your attendance.', 'error');
        window.openAuthModal('login');
        return;
      }

      const selectedMode = document.querySelector('input[name="attWorkMode"]:checked')?.value || 'Office';
      const punchNow = new Date();
      const formattedPunchTime = punchNow.toLocaleTimeString('en-US', { hour12: true, hour: '2-digit', minute: '2-digit', second: '2-digit' });

      try {
        const res = await fetch(`${API_BASE}/attendance/punch`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            userEmail: user.email,
            mode: selectedMode,
            punchTime: formattedPunchTime
          })
        });
        const json = await res.json();

        if (res.ok && json.success) {
          showToast(json.message, 'success');
          loadMyAttendance(user);
        } else {
          showToast(json.message || 'Error recording attendance.', 'error');
        }
      } catch (err) {
        showToast('Connection error communicating with attendance API.', 'error');
      }
    });
  }

  window.openAttendanceTracker = () => {
    const user = getCurrentUser();
    const notLoggedInView = document.getElementById('attNotLoggedInView');
    const loggedInView = document.getElementById('attLoggedInView');
    const mainTitle = document.getElementById('attModalMainTitle');
    const mainSubtitle = document.getElementById('attModalMainSubtitle');

    if (!user) {
      if (notLoggedInView) notLoggedInView.style.display = 'block';
      if (loggedInView) loggedInView.style.display = 'none';
      if (mainTitle) mainTitle.textContent = 'Personal Attendance & Shift Clock';
      if (mainSubtitle) mainSubtitle.textContent = 'Sign in to access your individual attendance records';
    } else {
      if (notLoggedInView) notLoggedInView.style.display = 'none';
      if (loggedInView) loggedInView.style.display = 'block';
      if (mainTitle) mainTitle.textContent = `${user.fullName}'s Attendance Clock`;
      if (mainSubtitle) mainSubtitle.textContent = `Personal shift tracker for ${user.email}`;

      const nameDisplay = document.getElementById('attUserNameDisplay');
      const emailDisplay = document.getElementById('attUserEmailDisplay');
      const deptDisplay = document.getElementById('attUserDeptDisplay');
      const roleBadge = document.getElementById('attUserRoleBadge');
      const avatarCircle = document.getElementById('attUserAvatarCircle');
      const adminPrompt = document.getElementById('adminPortalPromptNotice');

      if (nameDisplay) nameDisplay.textContent = user.fullName;
      if (emailDisplay) emailDisplay.textContent = user.email;
      if (deptDisplay) deptDisplay.textContent = user.department || 'General';
      if (roleBadge) {
        roleBadge.textContent = user.role;
        roleBadge.style.backgroundColor = user.role === 'ADMIN' ? '#fee2e2' : '#e0f2fe';
        roleBadge.style.color = user.role === 'ADMIN' ? '#ef4444' : '#0284c7';
      }
      if (avatarCircle) {
        avatarCircle.textContent = user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U';
        if (user.role === 'ADMIN') avatarCircle.style.backgroundColor = '#ef4444';
      }
      if (adminPrompt) {
        adminPrompt.style.display = user.role === 'ADMIN' ? 'flex' : 'none';
      }

      updateClockUI();
      if (clockInterval) clearInterval(clockInterval);
      clockInterval = setInterval(updateClockUI, 1000);

      loadMyAttendance(user);
    }

    if (attendanceModalBackdrop) {
      attendanceModalBackdrop.classList.add('open');
      document.body.style.overflow = 'hidden';
    }
  };

  window.closeAttendanceTrackerFunc = () => {
    if (clockInterval) {
      clearInterval(clockInterval);
      clockInterval = null;
    }
    const modal = document.getElementById('attendanceModalBackdrop');
    if (modal) {
      modal.classList.remove('open');
    }
    document.body.style.overflow = '';
  };
  window.closeAttendanceTracker = window.closeAttendanceTrackerFunc;

  const navbarAttBtn = document.getElementById('navbarAttendanceBtn');
  const closeAttModal = document.getElementById('closeAttendanceModal');
  const closeAttModalBtn = document.getElementById('closeAttendanceModalBtn');

  if (navbarAttBtn) navbarAttBtn.addEventListener('click', window.openAttendanceTracker);
  if (closeAttModal) {
    closeAttModal.addEventListener('click', (e) => {
      e.preventDefault();
      window.closeAttendanceTrackerFunc();
    });
  }
  if (closeAttModalBtn) {
    closeAttModalBtn.addEventListener('click', (e) => {
      e.preventDefault();
      window.closeAttendanceTrackerFunc();
    });
  }
  if (attendanceModalBackdrop) {
    attendanceModalBackdrop.addEventListener('click', (e) => {
      if (e.target === attendanceModalBackdrop) window.closeAttendanceTrackerFunc();
    });
  }

  // Escape key closes attendance tracker modal
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' || e.key === 'Esc') {
      const modal = document.getElementById('attendanceModalBackdrop');
      if (modal && modal.classList.contains('open')) {
        window.closeAttendanceTrackerFunc();
      }
    }
  });

  const DOMAIN_CAPABILITIES = [
    {
      domain: 'Time & Attendance Management',
      icon: '🕒',
      items: [
        { code: 'ATTENDANCE_CREATE', title: 'Self Shift Clock In / Clock Out', desc: 'Punch in and punch out your daily working shifts.' },
        { code: 'ATTENDANCE_VIEW', title: 'Company Attendance Timesheets', desc: 'Inspect organization-wide shift records and logs.' },
        { code: 'ATTENDANCE_EDIT', title: 'Shift Override & Force Punch-Out', desc: 'Perform administrative punch-outs or adjust shift times.' },
        { code: 'ATTENDANCE_DELETE', title: 'Delete Attendance Entries', desc: 'Permanently remove shift records from the database.' },
        { code: 'ATTENDANCE_EXPORT', title: 'Export Timesheets to CSV', desc: 'Download attendance spreadsheets for payroll & audit.' }
      ]
    },
    {
      domain: 'Workforce Directory & Profiles',
      icon: '👥',
      items: [
        { code: 'EMPLOYEE_VIEW', title: 'Workforce Directory Access', desc: 'Access employee directory information and personal profile.' },
        { code: 'EMPLOYEE_CREATE', title: 'Onboard New Employees', desc: 'Create new employee profiles in the workforce database.' },
        { code: 'EMPLOYEE_EDIT', title: 'Modify Employee Details', desc: 'Update designation, department, status, or contact details.' },
        { code: 'EMPLOYEE_DELETE', title: 'Deactivate / Remove Employees', desc: 'Terminate or remove employee records from directory.' },
        { code: 'EMPLOYEE_EXPORT', title: 'Export Workforce Roster', desc: 'Download complete employee roster to CSV spreadsheet.' }
      ]
    },
    {
      domain: 'Department Management',
      icon: '🏢',
      items: [
        { code: 'DEPARTMENT_VIEW', title: 'View Department Directory', desc: 'Browse company departments and organizational units.' },
        { code: 'DEPARTMENT_CREATE', title: 'Create New Departments', desc: 'Establish new organizational business units.' },
        { code: 'DEPARTMENT_EDIT', title: 'Edit Department Details', desc: 'Update department names, heads, and descriptions.' },
        { code: 'DEPARTMENT_DELETE', title: 'Delete Empty Departments', desc: 'Remove departments that have zero active members.' },
        { code: 'DEPARTMENT_EXPORT', title: 'Export Department Summary', desc: 'Export department headcount and structure data.' }
      ]
    },
    {
      domain: 'Reports & Analytics',
      icon: '📊',
      items: [
        { code: 'REPORT_VIEW', title: 'Operational Analytics & KPIs', desc: 'Inspect workforce metrics, shift trends, and charts.' },
        { code: 'REPORT_EXPORT', title: 'Download Executive Reports', desc: 'Export comprehensive business intelligence reports.' }
      ]
    },
    {
      domain: 'Identity & System Governance',
      icon: '🛡️',
      items: [
        { code: 'USER_VIEW', title: 'View Registered User Accounts', desc: 'Inspect user login accounts and verification status.' },
        { code: 'USER_MANAGE', title: 'User Account & Security Management', desc: 'Reset passwords, activate/deactivate user accounts.' },
        { code: 'ROLE_MANAGE', title: 'Role & Permission Matrix Control', desc: 'Create, modify roles and assign granular permissions.' },
        { code: 'CMS_VIEW', title: 'Preview Dynamic CMS Content', desc: 'View website landing page dynamic configurations.' },
        { code: 'CMS_EDIT', title: 'Modify Website Public CMS', desc: 'Edit website hero banner, about us, and company copy.' }
      ]
    }
  ];

  const ROLE_DESCRIPTIONS = {
    'ROLE_ADMIN': 'Full unrestricted administrative control over system data, user accounts, shifts, and security policies.',
    'ROLE_HR_MANAGER': 'Human resources manager with oversight of workforce rosters, attendance timesheets, and organizational reports.',
    'ROLE_MANAGER': 'Departmental manager supervising team workforce members and shift attendance.',
    'ROLE_USER': 'Standard employee access with personal attendance shift punching, profile management, and directory lookup.',
    'ROLE_VIEWER': 'Read-only audit access across workforce rosters, departments, and general performance reports.'
  };

  let currentActiveRoleFilter = 'ALL';
  let currentUserPermissionsSet = new Set();
  let currentIsAdmin = false;

  window.filterRoleCapabilities = function(filter) {
    currentActiveRoleFilter = filter;
    ['ALL', 'GRANTED', 'DENIED'].forEach(f => {
      const btn = document.getElementById(f === 'ALL' ? 'tabFilterAll' : (f === 'GRANTED' ? 'tabFilterGranted' : 'tabFilterDenied'));
      if (btn) btn.classList.toggle('active', f === filter);
    });
    renderRoleCapabilitiesList();
  };

  function renderRoleCapabilitiesList() {
    const container = document.getElementById('modalRoleCapabilitiesContainer');
    if (!container) return;

    let html = '';
    DOMAIN_CAPABILITIES.forEach(group => {
      const filteredItems = group.items.filter(item => {
        const isGranted = currentIsAdmin || currentUserPermissionsSet.has(item.code);
        if (currentActiveRoleFilter === 'GRANTED') return isGranted;
        if (currentActiveRoleFilter === 'DENIED') return !isGranted;
        return true;
      });

      if (filteredItems.length === 0) return;

      const groupGrantedCount = group.items.filter(item => currentIsAdmin || currentUserPermissionsSet.has(item.code)).length;

      html += `
        <div class="role-domain-group">
          <div class="role-domain-header">
            <div class="role-domain-title">
              <span>${group.icon}</span>
              <span>${escapeHtml(group.domain)}</span>
            </div>
            <span style="font-size: 11px; font-weight: 700; color: #475569; background: #e2e8f0; padding: 2px 8px; border-radius: 10px;">
              ${groupGrantedCount} / ${group.items.length} Authorized
            </span>
          </div>
          <div class="role-domain-grid">
            ${filteredItems.map(item => {
              const isGranted = currentIsAdmin || currentUserPermissionsSet.has(item.code);
              return `
                <div class="capability-card">
                  <div class="cap-info">
                    <span class="cap-title">${escapeHtml(item.title)}</span>
                    <span class="cap-code">${escapeHtml(item.code)}</span>
                    <span style="font-size: 11px; color: #64748b; margin-top: 2px;">${escapeHtml(item.desc)}</span>
                  </div>
                  <div>
                    ${isGranted 
                      ? `<span class="badge-perm-granted" title="Authorized for your active role">
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"></polyline></svg>
                          Authorized
                         </span>`
                      : `<span class="badge-perm-denied" title="Restricted - Requires elevated administrative clearance">
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                          Restricted
                         </span>`
                    }
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        </div>
      `;
    });

    if (!html) {
      html = `<div style="text-align: center; padding: 30px; color: #64748b; font-size: 13.5px; background: #f8fafc; border-radius: 8px; border: 1px dashed #cbd5e1;">No capabilities match the selected filter.</div>`;
    }

    container.innerHTML = html;
  }

  window.openUserRoleModal = async function() {
    let user = getCurrentUser();
    try {
      const res = await fetch(`${API_BASE}/auth/me`);
      if (res.ok) {
        const json = await res.json();
        if (json.data) {
          user = json.data;
          setCurrentUser(user);
        }
      }
    } catch (e) {}

    if (!user) {
      if (typeof window.openAuthModal === 'function') {
        window.openAuthModal('login');
      }
      return;
    }

    const modal = document.getElementById('userRoleModalBackdrop');
    if (!modal) return;

    // Fetch verified employee data strictly for this logged-in user
    let emp = null;
    try {
      const res = await fetch(`${API_BASE}/employees/me?email=${encodeURIComponent(user.email)}`);
      if (res.ok) {
        const json = await res.json();
        emp = json.data;
      }
    } catch (e) {}

    currentIsAdmin = (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN');
    currentUserPermissionsSet = new Set(user.permissions || []);

    const avatarEl = document.getElementById('modalUserAvatar');
    const nameEl = document.getElementById('modalUserFullName');
    const emailEl = document.getElementById('modalUserEmail');
    const empCodeEl = document.getElementById('modalUserEmpCode');
    const deptEl = document.getElementById('modalUserDept');
    const roleBadgeEl = document.getElementById('modalUserRoleBadge');
    const roleDescEl = document.getElementById('modalUserRoleDesc');
    const scopeEl = document.getElementById('modalUserScope');
    const adminPortalLink = document.getElementById('modalAdminPortalLink');
    const polpNotice = document.getElementById('modalPoLPNotice');

    const empName = emp?.name || user.fullName || user.email.split('@')[0];
    const empCode = emp?.employeeCode || 'EMP-001';
    const empDept = emp?.departmentName || (emp?.department && emp.department.name) || user.department || 'General';

    if (avatarEl) {
      avatarEl.textContent = empName.charAt(0).toUpperCase();
      avatarEl.style.backgroundColor = currentIsAdmin ? '#dc2626' : (user.role === 'ROLE_HR_MANAGER' ? '#7c3aed' : '#0284c7');
    }
    if (nameEl) nameEl.textContent = empName;
    if (emailEl) emailEl.textContent = user.email || '';
    if (empCodeEl) empCodeEl.textContent = empCode;
    if (deptEl) deptEl.textContent = empDept;

    if (roleBadgeEl) {
      if (currentIsAdmin) {
        roleBadgeEl.innerHTML = '👑 Tier 4: Super Administrator';
        roleBadgeEl.style.backgroundColor = '#fee2e2';
        roleBadgeEl.style.color = '#dc2626';
      } else if (user.role === 'ROLE_HR_MANAGER') {
        roleBadgeEl.innerHTML = '💼 Tier 3: HR Executive';
        roleBadgeEl.style.backgroundColor = '#ede9fe';
        roleBadgeEl.style.color = '#7c3aed';
      } else if (user.role === 'ROLE_MANAGER') {
        roleBadgeEl.innerHTML = '🏢 Tier 2: Department Supervisor';
        roleBadgeEl.style.backgroundColor = '#e0e7ff';
        roleBadgeEl.style.color = '#4338ca';
      } else if (user.role === 'ROLE_VIEWER') {
        roleBadgeEl.innerHTML = '👁️ Tier 0: Audit Observer';
        roleBadgeEl.style.backgroundColor = '#fef3c7';
        roleBadgeEl.style.color = '#b45309';
      } else {
        roleBadgeEl.innerHTML = `👤 Tier 1: ${escapeHtml(user.displayRole || 'Standard Employee')}`;
        roleBadgeEl.style.backgroundColor = '#e0f2fe';
        roleBadgeEl.style.color = '#0284c7';
      }
    }

    if (scopeEl) {
      if (currentIsAdmin) {
        scopeEl.textContent = 'Enterprise-Wide Governance (Unrestricted)';
      } else if (user.role === 'ROLE_HR_MANAGER') {
        scopeEl.textContent = 'Workforce Directory & Shift Oversight Scope';
      } else if (user.role === 'ROLE_MANAGER') {
        scopeEl.textContent = `Assigned ${empDept} Department Scope`;
      } else if (user.role === 'ROLE_VIEWER') {
        scopeEl.textContent = 'Read-Only Company Audit Scope';
      } else {
        scopeEl.textContent = 'Personal Workforce Record & Assigned Shift Scope';
      }
    }

    const roleKey = user.role || (currentIsAdmin ? 'ROLE_ADMIN' : 'ROLE_USER');
    if (roleDescEl) {
      roleDescEl.textContent = ROLE_DESCRIPTIONS[roleKey] || (user.roleEntity && user.roleEntity.description) || 'Enterprise access policy configured by system administrator.';
    }

    // Calculate granted and restricted statistics
    let totalCaps = 0;
    let grantedCaps = 0;
    DOMAIN_CAPABILITIES.forEach(group => {
      group.items.forEach(item => {
        totalCaps++;
        if (currentIsAdmin || currentUserPermissionsSet.has(item.code)) {
          grantedCaps++;
        }
      });
    });
    const deniedCaps = totalCaps - grantedCaps;

    const countAllEl = document.getElementById('tabCountAll');
    const countGrantedEl = document.getElementById('tabCountGranted');
    const countDeniedEl = document.getElementById('tabCountDenied');

    if (countAllEl) countAllEl.textContent = `(${totalCaps})`;
    if (countGrantedEl) countGrantedEl.textContent = `(${grantedCaps})`;
    if (countDeniedEl) countDeniedEl.textContent = `(${deniedCaps})`;

    filterRoleCapabilities('ALL');

    if (adminPortalLink) {
      const canManage = currentIsAdmin || currentUserPermissionsSet.has('ROLE_MANAGE');
      adminPortalLink.style.display = canManage ? 'inline-flex' : 'none';
      if (polpNotice) polpNotice.style.display = canManage ? 'none' : 'inline-flex';
    }

    modal.classList.add('open');
    document.body.style.overflow = 'hidden';
  };

  const closeUserRoleModalBtn = document.getElementById('closeUserRoleModal');
  const closeUserRoleModalBtnFooter = document.getElementById('closeUserRoleModalBtn');
  const userRoleModalBackdrop = document.getElementById('userRoleModalBackdrop');

  if (closeUserRoleModalBtn) closeUserRoleModalBtn.addEventListener('click', window.closeUserRoleModal);
  if (closeUserRoleModalBtnFooter) closeUserRoleModalBtnFooter.addEventListener('click', window.closeUserRoleModal);
  if (userRoleModalBackdrop) {
    userRoleModalBackdrop.addEventListener('click', (e) => {
      if (e.target === userRoleModalBackdrop) window.closeUserRoleModal();
    });
  }

  updateNavbarAuth();
  loadCmsContent();
  loadDepartments();

  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('error') === 'unauthorized') {
    showToast('Administrator login required to access Admin Control Center.', 'error');
    if (typeof window.openAuthModal === 'function') {
      window.openAuthModal('login');
      const r = document.getElementById('loginRoleAdmin');
      if (r) r.checked = true;
      const emailInput = document.getElementById('loginEmailInput');
      if (emailInput && !emailInput.value) emailInput.value = 'admin@empmanage.com';
    }
  } else if (urlParams.get('error') === 'forbidden') {
    showToast('Access denied. Administrator privileges required.', 'error');
  }
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