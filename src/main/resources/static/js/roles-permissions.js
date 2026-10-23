/**
 * EmpManage Enterprise - Role & Permission Studio
 * High-Performance Vanilla JavaScript Client
 */

document.addEventListener('DOMContentLoaded', async () => {

  const API_BASE = '/api';

  // State Management
  let currentUser = null;
  let rolesCache = [];
  let usersCache = [];
  let auditLogsCache = [];
  let activeRoleId = null;
  let activeRolePermissions = new Set();
  let currentWorkspaceTab = 'studio';

  // Toast Notification System
  const showToast = (message, type = 'info') => {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast-msg ${type}`;
    const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
    toast.innerHTML = `<span>${icon}</span><span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(100%)';
      toast.style.transition = 'all 0.25s ease';
      setTimeout(() => toast.remove(), 250);
    }, 4000);
  };

  // 1. Session Verification
  const verifyAccess = async () => {
    try {
      const res = await fetch(`${API_BASE}/auth/me`);
      if (res.ok) {
        const json = await res.json();
        const user = json.data;
        const isAdmin = user && (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN' || (user.role && user.role.toUpperCase().includes('ADMIN')) || user.displayRole === 'ADMIN' || (user.permissions && user.permissions.includes('ROLE_MANAGE')));
        if (isAdmin) {
          currentUser = user;
          const adminHeaderName = document.getElementById('adminHeaderName');
          if (adminHeaderName) adminHeaderName.textContent = currentUser.fullName || 'Administrator';
          return true;
        }
      }
    } catch (e) {
      console.warn('Backend authentication check failed:', e);
    }

    // Fallback: Check local storage
    try {
      const local = JSON.parse(localStorage.getItem('empmanage_session_user') || 'null');
      const isLocalAdmin = local && (local.role === 'ROLE_ADMIN' || local.role === 'ADMIN' || (local.role && local.role.toUpperCase().includes('ADMIN')) || local.displayRole === 'ADMIN' || (local.permissions && local.permissions.includes('ROLE_MANAGE')));
      if (isLocalAdmin) {
        currentUser = local;
        const adminHeaderName = document.getElementById('adminHeaderName');
        if (adminHeaderName) adminHeaderName.textContent = currentUser.fullName || 'Administrator';
        return true;
      }
    } catch (e) {}

    window.location.replace('index.html?error=unauthorized');
    return false;
  };

  const hasAccess = await verifyAccess();
  if (!hasAccess) return;

  // Logout Handler
  const adminLogoutBtn = document.getElementById('adminLogoutBtn');
  if (adminLogoutBtn) {
    adminLogoutBtn.addEventListener('click', async () => {
      try {
        await fetch(`${API_BASE}/auth/logout`, { method: 'POST' });
      } catch (e) {}
      localStorage.removeItem('empmanage_session_user');
      showToast('Logged out of Admin Portal.', 'info');
      setTimeout(() => window.location.replace('index.html'), 400);
    });
  }

  // =========================================================
  // 2. 7 REAL CORE MODULES & 22 GRANULAR PERMISSIONS
  // =========================================================
  const MODULE_DEFINITIONS = [
    {
      key: 'EMPLOYEES',
      title: 'Workforce Employees',
      icon: '👥',
      description: 'Employee roster, directories, profile details, and staff onboarding.',
      permissions: [
        { code: 'EMPLOYEE_VIEW', name: 'View Workforce Directory', desc: 'Browse employee profiles, team members, and directory details.' },
        { code: 'EMPLOYEE_CREATE', name: 'Create Employee Records', desc: 'Onboard new personnel and add employee profiles.' },
        { code: 'EMPLOYEE_EDIT', name: 'Edit Employee Details', desc: 'Update designation, department, contact info, and status.' },
        { code: 'EMPLOYEE_DELETE', name: 'Remove Employee Records', desc: 'Permanently remove or terminate workforce profiles.' },
        { code: 'EMPLOYEE_EXPORT', name: 'Export Directory (CSV)', desc: 'Download complete employee roster to CSV spreadsheet.' }
      ]
    },
    {
      key: 'DEPARTMENTS',
      title: 'Organizational Departments',
      icon: '🏢',
      description: 'Departmental hierarchy, team structures, and business units.',
      permissions: [
        { code: 'DEPARTMENT_VIEW', name: 'View Departments', desc: 'Inspect company departments and team member counts.' },
        { code: 'DEPARTMENT_CREATE', name: 'Create Departments', desc: 'Establish new company business divisions.' },
        { code: 'DEPARTMENT_EDIT', name: 'Edit Department Info', desc: 'Update department names, heads, and descriptions.' },
        { code: 'DEPARTMENT_DELETE', name: 'Delete Empty Departments', desc: 'Remove departments that have zero assigned members.' },
        { code: 'DEPARTMENT_EXPORT', name: 'Export Departments (CSV)', desc: 'Download departmental statistics to CSV file.' }
      ]
    },
    {
      key: 'ATTENDANCE',
      title: 'Time & Attendance Shifts',
      icon: '🕒',
      description: 'Shift punch clocks, attendance logs, and working timesheets.',
      permissions: [
        { code: 'ATTENDANCE_VIEW', name: 'View Company Shifts', desc: 'Inspect organization-wide employee punch clocks.' },
        { code: 'ATTENDANCE_CREATE', name: 'Self Shift Punch Clock', desc: 'Clock in and punch out personal daily working shifts.' },
        { code: 'ATTENDANCE_EDIT', name: 'Shift Override & Punch-Out', desc: 'Admin punch-out for ongoing shifts and time edits.' },
        { code: 'ATTENDANCE_DELETE', name: 'Delete Shift Logs', desc: 'Remove corrupted or invalid attendance entries.' },
        { code: 'ATTENDANCE_EXPORT', name: 'Export Timesheets (CSV)', desc: 'Download detailed attendance spreadsheets for payroll.' }
      ]
    },
    {
      key: 'REPORTS',
      title: 'Reports & Analytics',
      icon: '📊',
      description: 'Operational shift metrics, workforce KPIs, and business intelligence.',
      permissions: [
        { code: 'REPORT_VIEW', name: 'View Operational Reports', desc: 'Inspect live charts, attendance ratios, and workforce stats.' },
        { code: 'REPORT_EXPORT', name: 'Download Comprehensive Reports', desc: 'Export executive analytical spreadsheets and logs.' }
      ]
    },
    {
      key: 'CMS',
      title: 'Public Portal & Website CMS',
      icon: '📝',
      description: 'Public landing page banner, about section, and dynamic content.',
      permissions: [
        { code: 'CMS_VIEW', name: 'Preview Dynamic CMS', desc: 'Preview public portal CMS sections and announcements.' },
        { code: 'CMS_EDIT', name: 'Modify Website CMS Content', desc: 'Edit hero titles, subtitle text, features, and about copy.' }
      ]
    },
    {
      key: 'USERS',
      title: 'User Accounts & Access',
      icon: '👤',
      description: 'User authentication credentials, account statuses, and passwords.',
      permissions: [
        { code: 'USER_VIEW', name: 'View User Accounts', desc: 'Inspect registered logins, emails, and account activity.' },
        { code: 'USER_MANAGE', name: 'Manage Credentials & Status', desc: 'Reset passwords, activate/deactivate user accounts.' }
      ]
    },
    {
      key: 'ROLES',
      title: 'Role Governance & RBAC',
      icon: '🛡️',
      description: 'System roles, custom authorization policies, and permission matrix.',
      permissions: [
        { code: 'ROLE_MANAGE', name: 'Configure Roles & Permission Matrix', desc: 'Create, modify roles and grant granular permission policies.' }
      ]
    }
  ];

  const TOTAL_SYSTEM_PERMS = 22;

  const getRoleIcon = (roleName) => {
    const r = (roleName || '').toUpperCase();
    if (r === 'ROLE_ADMIN' || r === 'ADMIN') return '👑';
    if (r === 'ROLE_HR_MANAGER') return '💼';
    if (r === 'ROLE_MANAGER') return '🏢';
    if (r === 'ROLE_USER') return '👤';
    if (r === 'ROLE_VIEWER') return '👁️';
    return '⚙️';
  };

  // =========================================================
  // 3. WORKSPACE TABS SWITCHER
  // =========================================================
  window.switchWorkspaceTab = (tab) => {
    currentWorkspaceTab = tab;

    const views = {
      studio: document.getElementById('viewWorkspaceStudio'),
      users: document.getElementById('viewWorkspaceUsers'),
      audit: document.getElementById('viewWorkspaceAudit')
    };

    const btns = {
      studio: document.getElementById('tabBtnStudio'),
      users: document.getElementById('tabBtnUsers'),
      audit: document.getElementById('tabBtnAudit')
    };

    Object.keys(views).forEach(k => {
      if (views[k]) views[k].style.display = (k === tab) ? 'block' : 'none';
      if (btns[k]) btns[k].classList.toggle('active', k === tab);
    });

    if (tab === 'users' && usersCache.length === 0) {
      loadUsers();
    } else if (tab === 'audit' && auditLogsCache.length === 0) {
      loadAuditLogs();
    }
  };

  // =========================================================
  // 4. LOAD DASHBOARD METRICS
  // =========================================================
  const loadDashboardStats = async () => {
    try {
      const res = await fetch(`${API_BASE}/roles/stats`);
      if (res.ok) {
        const json = await res.json();
        const s = json.data;
        document.getElementById('statTotalRoles').textContent = s.totalRoles || 0;
        document.getElementById('statTotalPermissions').textContent = s.totalPermissions || TOTAL_SYSTEM_PERMS;
        document.getElementById('statAssignedUsers').textContent = s.assignedUsers || 0;
        
        const rPill = document.getElementById('tabRoleCountPill');
        if (rPill) rPill.textContent = `${s.totalRoles || 0} Roles`;
        
        const uPill = document.getElementById('tabUserCountPill');
        if (uPill) uPill.textContent = `${s.assignedUsers || 0} Users`;
      }
    } catch (e) {
      console.error('Error loading stats:', e);
    }
  };

  // =========================================================
  // 5. LOAD ROLES & POPULATE STUDIO SIDEBAR
  // =========================================================
  const loadRoles = async () => {
    try {
      const res = await fetch(`${API_BASE}/roles`);
      if (res.ok) {
        const json = await res.json();
        rolesCache = json.data || [];
        renderStudioRolesSidebar(rolesCache);

        // Default active role
        if (!activeRoleId && rolesCache.length > 0) {
          const adminRole = rolesCache.find(r => r.name === 'ROLE_ADMIN') || rolesCache[0];
          selectRoleInStudio(adminRole.id);
        } else if (activeRoleId) {
          const found = rolesCache.find(r => r.id === activeRoleId);
          if (found) {
            selectRoleInStudio(found.id);
          } else if (rolesCache.length > 0) {
            selectRoleInStudio(rolesCache[0].id);
          }
        }
      }
    } catch (e) {
      console.error('Error loading roles:', e);
      showToast('Failed to load roles from server.', 'error');
    }
  };

  const renderStudioRolesSidebar = (roles) => {
    const container = document.getElementById('studioRolesContainer');
    if (!container) return;

    if (!roles || roles.length === 0) {
      container.innerHTML = `<div style="padding:20px; text-align:center; color:#94a3b8; font-size:13px;">No roles found.</div>`;
      return;
    }

    container.innerHTML = roles.map(r => {
      const isSelected = r.id === activeRoleId;
      const icon = getRoleIcon(r.name);
      const permCount = (r.permissions || []).length;
      const typeBadge = r.systemRole
        ? `<span class="badge-system">System</span>`
        : `<span class="badge-custom">Custom</span>`;

      return `
        <div class="role-card-item ${isSelected ? 'active' : ''}" onclick="window.selectRoleInStudio(${r.id})">
          <div class="role-card-top">
            <span class="role-card-title">
              <span>${icon}</span>
              <span>${escapeHtml(r.displayName)}</span>
            </span>
            ${typeBadge}
          </div>
          <div style="font-size:11px; font-family:'JetBrains Mono', monospace; color:#64748b;">
            ${escapeHtml(r.name)}
          </div>
          <div class="role-card-meta">
            <span style="font-weight:700; color:${r.active ? '#15803d' : '#94a3b8'};">
              ${r.active ? '● Active' : '○ Inactive'}
            </span>
            <span>${r.assignedUsersCount || 0} users</span>
            <span style="background:${isSelected ? '#bae6fd' : '#e2e8f0'}; color:${isSelected ? '#0369a1' : '#475569'}; padding:1px 6px; border-radius:10px; font-weight:700; font-size:11px;">
              ${permCount} / ${TOTAL_SYSTEM_PERMS}
            </span>
          </div>
        </div>
      `;
    }).join('');
  };

  // Search filter for roles sidebar
  const studioRoleSearchInput = document.getElementById('studioRoleSearchInput');
  if (studioRoleSearchInput) {
    studioRoleSearchInput.addEventListener('input', (e) => {
      const q = e.target.value.toLowerCase().trim();
      const filtered = rolesCache.filter(r => 
        r.name.toLowerCase().includes(q) ||
        r.displayName.toLowerCase().includes(q) ||
        (r.description && r.description.toLowerCase().includes(q))
      );
      renderStudioRolesSidebar(filtered);
    });
  }

  // =========================================================
  // 6. SELECT ROLE IN STUDIO & POPULATE EDITOR
  // =========================================================
  window.selectRoleInStudio = (roleId) => {
    const role = rolesCache.find(r => r.id === roleId);
    if (!role) return;

    activeRoleId = role.id;
    activeRolePermissions = new Set(role.permissions || []);

    // Update active highlight in sidebar
    renderStudioRolesSidebar(rolesCache);

    // Update Hero Banner
    document.getElementById('studioRoleIcon').textContent = getRoleIcon(role.name);
    document.getElementById('studioRoleDisplayName').textContent = role.displayName;
    document.getElementById('studioRoleCode').textContent = role.name;
    document.getElementById('studioRoleDesc').textContent = role.description || 'Enterprise role policy configured in PostgreSQL.';

    const typeBadge = document.getElementById('studioRoleTypeBadge');
    if (typeBadge) {
      typeBadge.className = role.systemRole ? 'badge-system' : 'badge-custom';
      typeBadge.textContent = role.systemRole ? 'SYSTEM ROLE' : 'CUSTOM ROLE';
    }

    // Role Actions Row (Edit / Toggle / Delete)
    const actionsRow = document.getElementById('studioRoleActionsRow');
    if (actionsRow) {
      let buttonsHtml = '';
      
      // Edit button (for all roles except restricted system attributes)
      buttonsHtml += `
        <button type="button" class="btn btn-outline" onclick="window.openEditRoleModal(${role.id})" style="padding:6px 12px; font-size:12.5px; border-color:rgba(255,255,255,0.3); color:#ffffff; border-radius:6px; cursor:pointer;" title="Edit Role Details">
          ✏️ Edit Details
        </button>
      `;

      // Status toggle (active / inactive) - disabled for ROLE_ADMIN
      if (role.name !== 'ROLE_ADMIN') {
        buttonsHtml += `
          <button type="button" class="btn btn-outline" onclick="window.toggleRoleStatus(${role.id}, ${!role.active})" style="padding:6px 12px; font-size:12.5px; border-color:rgba(255,255,255,0.3); color:#ffffff; border-radius:6px; cursor:pointer;" title="${role.active ? 'Deactivate Role' : 'Activate Role'}">
            ${role.active ? '⏸️ Deactivate' : '▶️ Activate'}
          </button>
        `;
      }

      // Delete button (Custom roles only with 0 users)
      if (!role.systemRole) {
        const canDelete = (role.assignedUsersCount || 0) === 0;
        buttonsHtml += `
          <button type="button" class="btn btn-outline" onclick="window.deleteRole(${role.id})" ${canDelete ? '' : 'disabled'} style="padding:6px 12px; font-size:12.5px; border-color:#fca5a5; color:#fca5a5; border-radius:6px; cursor:pointer;" title="${canDelete ? 'Permanently Delete Custom Role' : 'Cannot delete role with assigned users'}">
            🗑️ Delete
          </button>
        `;
      }

      actionsRow.innerHTML = buttonsHtml;
    }

    updateStudioStatsText();
    renderStudioModules();
  };

  const updateStudioStatsText = () => {
    const statsEl = document.getElementById('studioGrantedStats');
    if (statsEl) {
      statsEl.textContent = `${activeRolePermissions.size} of ${TOTAL_SYSTEM_PERMS} Authorized`;
    }
  };

  // =========================================================
  // 7. RENDER ROLE PERMISSIONS AS PROFESSIONAL DATA TABLES
  // =========================================================
  let currentStudioTableView = 'matrix'; // 'matrix' or 'detailed'

  window.setStudioTableView = (mode) => {
    currentStudioTableView = mode;
    const btnMatrix = document.getElementById('btnViewMatrix');
    const btnDetailed = document.getElementById('btnViewDetailed');
    if (btnMatrix) btnMatrix.classList.toggle('active', mode === 'matrix');
    if (btnDetailed) btnDetailed.classList.toggle('active', mode === 'detailed');
    renderStudioModules();
  };

  const MATRIX_MODULES = [
    {
      key: 'EMPLOYEES',
      title: 'Workforce Employees',
      icon: '👥',
      description: 'Employee roster, directories, profile details, and staff onboarding.',
      actions: {
        VIEW: { code: 'EMPLOYEE_VIEW', name: 'View Workforce Directory' },
        CREATE: { code: 'EMPLOYEE_CREATE', name: 'Create Employee Records' },
        EDIT: { code: 'EMPLOYEE_EDIT', name: 'Edit Employee Details' },
        DELETE: { code: 'EMPLOYEE_DELETE', name: 'Remove Employee Records' },
        EXPORT: { code: 'EMPLOYEE_EXPORT', name: 'Export Directory (CSV)' }
      }
    },
    {
      key: 'DEPARTMENTS',
      title: 'Organizational Departments',
      icon: '🏢',
      description: 'Departmental hierarchy, team structures, and business units.',
      actions: {
        VIEW: { code: 'DEPARTMENT_VIEW', name: 'View Departments' },
        CREATE: { code: 'DEPARTMENT_CREATE', name: 'Create Departments' },
        EDIT: { code: 'DEPARTMENT_EDIT', name: 'Edit Department Info' },
        DELETE: { code: 'DEPARTMENT_DELETE', name: 'Delete Empty Departments' },
        EXPORT: { code: 'DEPARTMENT_EXPORT', name: 'Export Departments (CSV)' }
      }
    },
    {
      key: 'ATTENDANCE',
      title: 'Time & Attendance Shifts',
      icon: '🕒',
      description: 'Shift punch clocks, attendance logs, and working timesheets.',
      actions: {
        VIEW: { code: 'ATTENDANCE_VIEW', name: 'View Company Shifts' },
        CREATE: { code: 'ATTENDANCE_CREATE', name: 'Self Shift Punch Clock' },
        EDIT: { code: 'ATTENDANCE_EDIT', name: 'Shift Override & Punch-Out' },
        DELETE: { code: 'ATTENDANCE_DELETE', name: 'Delete Shift Logs' },
        EXPORT: { code: 'ATTENDANCE_EXPORT', name: 'Export Timesheets (CSV)' }
      }
    },
    {
      key: 'REPORTS',
      title: 'Reports & Analytics',
      icon: '📊',
      description: 'Operational shift metrics, workforce KPIs, and business intelligence.',
      actions: {
        VIEW: { code: 'REPORT_VIEW', name: 'View Operational Reports' },
        EXPORT: { code: 'REPORT_EXPORT', name: 'Download Comprehensive Reports' }
      }
    },
    {
      key: 'CMS',
      title: 'Public Portal & Website CMS',
      icon: '📝',
      description: 'Public landing page banner, about section, and dynamic content.',
      actions: {
        VIEW: { code: 'CMS_VIEW', name: 'Preview Dynamic CMS' },
        EDIT: { code: 'CMS_EDIT', name: 'Modify Website CMS Content' }
      }
    },
    {
      key: 'USERS',
      title: 'User Accounts & Access',
      icon: '👤',
      description: 'User authentication credentials, account statuses, and passwords.',
      actions: {
        VIEW: { code: 'USER_VIEW', name: 'View User Accounts' },
        EDIT: { code: 'USER_MANAGE', name: 'Manage Credentials & Status' }
      }
    },
    {
      key: 'ROLES',
      title: 'Role Governance & RBAC',
      icon: '🛡️',
      description: 'System roles, custom authorization policies, and permission matrix.',
      actions: {
        EDIT: { code: 'ROLE_MANAGE', name: 'Configure Roles & Permission Matrix' }
      }
    }
  ];

  const renderMatrixTable = () => {
    const actionCols = [
      { key: 'VIEW', title: '👁️ View', short: 'View' },
      { key: 'CREATE', title: '➕ Create', short: 'Create' },
      { key: 'EDIT', title: '✏️ Edit', short: 'Edit' },
      { key: 'DELETE', title: '🗑️ Delete', short: 'Delete' },
      { key: 'EXPORT', title: '📥 Export', short: 'Export' }
    ];

    let html = `
      <table class="matrix-table">
        <thead>
          <tr>
            <th style="width:28%;">System Module &amp; Scope</th>
            ${actionCols.map(col => `
              <th style="width:12%; text-align:center;">
                <div class="col-head-title">${col.title}</div>
                <button type="button" class="col-toggle-btn" onclick="window.toggleColumnAction('${col.key}')" title="Toggle all ${col.short} permissions">Toggle</button>
              </th>
            `).join('')}
            <th style="width:12%; text-align:center;">Module Controls</th>
          </tr>
        </thead>
        <tbody>
    `;

    MATRIX_MODULES.forEach(mod => {
      const modPermCodes = Object.values(mod.actions).map(a => a.code);
      const modGrantedCount = modPermCodes.filter(c => activeRolePermissions.has(c)).length;
      const modTotalCount = modPermCodes.length;

      html += `
        <tr>
          <td>
            <div class="matrix-module-info">
              <div class="matrix-module-icon">${mod.icon}</div>
              <div>
                <div class="matrix-module-name">${escapeHtml(mod.title)}</div>
                <div class="matrix-module-sub">${escapeHtml(mod.description)}</div>
              </div>
            </div>
          </td>
      `;

      actionCols.forEach(col => {
        const actionObj = mod.actions[col.key];
        if (actionObj) {
          const isChecked = activeRolePermissions.has(actionObj.code);
          html += `
            <td class="matrix-cell ${isChecked ? 'granted' : ''}" id="matrixCell_${actionObj.code}">
              <label class="matrix-checkbox-wrap" title="${escapeHtml(actionObj.name)} (${actionObj.code})">
                <input type="checkbox" class="matrix-checkbox" data-perm="${actionObj.code}" ${isChecked ? 'checked' : ''} onchange="window.togglePermission('${actionObj.code}', this.checked)">
                <span class="matrix-action-name" id="matrixLabel_${actionObj.code}">${isChecked ? '✓ Granted' : 'Allow'}</span>
              </label>
            </td>
          `;
        } else {
          html += `
            <td class="matrix-cell na" title="Action not applicable for this module">
              <span class="na-dash">—</span>
            </td>
          `;
        }
      });

      html += `
          <td style="text-align:center;">
            <div class="matrix-row-controls">
              <span class="matrix-badge" id="moduleBadge_${mod.key}">${modGrantedCount}/${modTotalCount}</span>
              <button type="button" class="btn-matrix-quick" onclick="window.toggleModulePermissions('${mod.key}', true)" title="Grant all in ${escapeHtml(mod.title)}">All</button>
              <button type="button" class="btn-matrix-quick clear" onclick="window.toggleModulePermissions('${mod.key}', false)" title="Revoke all in ${escapeHtml(mod.title)}">Clear</button>
            </div>
          </td>
        </tr>
      `;
    });

    html += `
        </tbody>
      </table>
    `;
    return html;
  };

  const renderDetailedTable = () => {
    let html = `
      <table class="detailed-module-table">
        <thead>
          <tr>
            <th style="width:20%;">System Module</th>
            <th style="width:22%;">Capability Name</th>
            <th style="width:20%;">Permission Key</th>
            <th style="width:26%;">Description &amp; Operational Scope</th>
            <th style="width:12%; text-align:center;">Authorization</th>
          </tr>
        </thead>
        <tbody>
    `;

    MODULE_DEFINITIONS.forEach(mod => {
      mod.permissions.forEach((perm, idx) => {
        const isChecked = activeRolePermissions.has(perm.code);
        html += `
          <tr class="${isChecked ? 'row-granted' : ''}" id="detailedRow_${perm.code}">
            <td>
              ${idx === 0 ? `
                <div style="display:flex; align-items:center; gap:8px; font-weight:700; color:var(--navy-dark);">
                  <span style="font-size:16px;">${mod.icon}</span>
                  <span>${escapeHtml(mod.title)}</span>
                </div>
              ` : `
                <span style="font-size:11px; color:#94a3b8; padding-left:24px;">↳ ${escapeHtml(mod.title)}</span>
              `}
            </td>
            <td style="font-weight:600; color:#1e293b;">
              ${escapeHtml(perm.name)}
            </td>
            <td>
              <span class="editor-role-code" style="font-size:11px;">${escapeHtml(perm.code)}</span>
            </td>
            <td style="font-size:12px; color:#64748b;">
              ${escapeHtml(perm.desc)}
            </td>
            <td style="text-align:center;">
              <label class="matrix-checkbox-wrap" style="justify-content:center;">
                <input type="checkbox" class="matrix-checkbox" data-perm="${perm.code}" ${isChecked ? 'checked' : ''} onchange="window.togglePermission('${perm.code}', this.checked)">
                <span class="cap-status-pill ${isChecked ? 'granted' : 'restricted'}" id="detailedPill_${perm.code}">
                  ${isChecked ? '✓ Granted' : 'Restricted'}
                </span>
              </label>
            </td>
          </tr>
        `;
      });
    });

    html += `
        </tbody>
      </table>
    `;
    return html;
  };

  const renderStudioModules = () => {
    const container = document.getElementById('studioModulesContainer');
    if (!container) return;

    if (currentStudioTableView === 'detailed') {
      container.innerHTML = renderDetailedTable();
    } else {
      container.innerHTML = renderMatrixTable();
    }
  };

  // Toggle single permission with dynamic live update for table cells
  window.togglePermission = (permCode, isChecked) => {
    if (isChecked) {
      activeRolePermissions.add(permCode);
    } else {
      activeRolePermissions.delete(permCode);
    }

    // Live update Matrix table elements
    const cell = document.getElementById(`matrixCell_${permCode}`);
    if (cell) cell.classList.toggle('granted', isChecked);

    const label = document.getElementById(`matrixLabel_${permCode}`);
    if (label) label.textContent = isChecked ? '✓ Granted' : 'Allow';

    // Live update Detailed table elements
    const row = document.getElementById(`detailedRow_${permCode}`);
    if (row) row.classList.toggle('row-granted', isChecked);

    const pill = document.getElementById(`detailedPill_${permCode}`);
    if (pill) {
      pill.className = `cap-status-pill ${isChecked ? 'granted' : 'restricted'}`;
      pill.textContent = isChecked ? '✓ Granted' : 'Restricted';
    }

    // Keep checkboxes synchronized
    document.querySelectorAll(`input.matrix-checkbox[data-perm="${permCode}"]`).forEach(cb => {
      cb.checked = isChecked;
    });

    // Update module counter badge in matrix table
    MATRIX_MODULES.forEach(mod => {
      const modPermCodes = Object.values(mod.actions).map(a => a.code);
      if (modPermCodes.includes(permCode)) {
        const badge = document.getElementById(`moduleBadge_${mod.key}`);
        if (badge) {
          const modGrantedCount = modPermCodes.filter(c => activeRolePermissions.has(c)).length;
          badge.textContent = `${modGrantedCount}/${modPermCodes.length}`;
        }
      }
    });

    updateStudioStatsText();
  };

  // Toggle entire module in the table
  window.toggleModulePermissions = (moduleKey, grant) => {
    const mod = MATRIX_MODULES.find(m => m.key === moduleKey);
    if (!mod) return;

    Object.values(mod.actions).forEach(actionObj => {
      const permCode = actionObj.code;
      if (grant) activeRolePermissions.add(permCode);
      else activeRolePermissions.delete(permCode);

      const cell = document.getElementById(`matrixCell_${permCode}`);
      if (cell) cell.classList.toggle('granted', grant);

      const label = document.getElementById(`matrixLabel_${permCode}`);
      if (label) label.textContent = grant ? '✓ Granted' : 'Allow';

      const row = document.getElementById(`detailedRow_${permCode}`);
      if (row) row.classList.toggle('row-granted', grant);

      const pill = document.getElementById(`detailedPill_${permCode}`);
      if (pill) {
        pill.className = `cap-status-pill ${grant ? 'granted' : 'restricted'}`;
        pill.textContent = grant ? '✓ Granted' : 'Restricted';
      }

      document.querySelectorAll(`input.matrix-checkbox[data-perm="${permCode}"]`).forEach(cb => {
        cb.checked = grant;
      });
    });

    const badge = document.getElementById(`moduleBadge_${mod.key}`);
    if (badge) {
      const total = Object.keys(mod.actions).length;
      badge.textContent = grant ? `${total}/${total}` : `0/${total}`;
    }

    updateStudioStatsText();
  };

  // Toggle all permissions belonging to a column action across all modules (View, Create, Edit, Delete, Export)
  window.toggleColumnAction = (actionType) => {
    const colPerms = [];
    MATRIX_MODULES.forEach(mod => {
      if (mod.actions[actionType]) {
        colPerms.push(mod.actions[actionType].code);
      }
    });

    if (colPerms.length === 0) return;

    const allGranted = colPerms.every(code => activeRolePermissions.has(code));
    const targetState = !allGranted;

    colPerms.forEach(code => {
      if (targetState) {
        activeRolePermissions.add(code);
      } else {
        activeRolePermissions.delete(code);
      }
    });

    renderStudioModules();
    updateStudioStatsText();
    showToast(`${actionType} permissions ${targetState ? 'granted' : 'revoked'}.`, 'info');
  };

  // Select All in Studio
  const selectAllStudioPermissions = () => {
    MODULE_DEFINITIONS.forEach(mod => {
      mod.permissions.forEach(p => activeRolePermissions.add(p.code));
    });
    renderStudioModules();
    updateStudioStatsText();
    showToast('All 22 permissions selected.', 'info');
  };

  // Deselect All in Studio
  const deselectAllStudioPermissions = () => {
    activeRolePermissions.clear();
    renderStudioModules();
    updateStudioStatsText();
    showToast('All permissions cleared for active role.', 'info');
  };

  const btnStudioSelectAll = document.getElementById('btnStudioSelectAll');
  if (btnStudioSelectAll) btnStudioSelectAll.addEventListener('click', selectAllStudioPermissions);

  const btnStudioDeselectAll = document.getElementById('btnStudioDeselectAll');
  if (btnStudioDeselectAll) btnStudioDeselectAll.addEventListener('click', deselectAllStudioPermissions);

  // Save Permissions Handler
  const saveStudioPermissions = async () => {
    if (!activeRoleId) {
      showToast('Please select a role to configure.', 'error');
      return;
    }

    const role = rolesCache.find(r => r.id === activeRoleId);
    const saveBtns = [
      document.getElementById('btnStudioSaveTop'),
      document.getElementById('btnStudioSaveBottom')
    ];

    saveBtns.forEach(btn => {
      if (btn) {
        btn.disabled = true;
        btn.innerHTML = `<span>⏳</span> Saving...`;
      }
    });

    try {
      const permsArray = Array.from(activeRolePermissions);
      const res = await fetch(`${API_BASE}/roles/${activeRoleId}/permissions`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ permissions: permsArray })
      });

      const json = await res.json();
      if (res.ok) {
        showToast(`Permissions saved for role '${role ? role.displayName : ''}'!`, 'success');
        
        // Update local cache
        if (role) role.permissions = permsArray;
        renderStudioRolesSidebar(rolesCache);

        // Immediately refresh active session in localStorage
        try {
          const meRes = await fetch(`${API_BASE}/auth/me`);
          if (meRes.ok) {
            const meJson = await meRes.json();
            if (meJson.data) {
              localStorage.setItem('empmanage_session_user', JSON.stringify(meJson.data));
            }
          }
        } catch (_) {}

        await Promise.all([
          loadDashboardStats(),
          loadAuditLogs(),
          loadUsers()
        ]);
      } else {
        showToast(json.message || 'Failed to save permissions.', 'error');
      }
    } catch (err) {
      showToast('Server error while saving permissions.', 'error');
    } finally {
      saveBtns.forEach(btn => {
        if (btn) {
          btn.disabled = false;
          btn.innerHTML = `<span>💾</span> Save Changes`;
        }
      });
    }
  };

  const btnSaveTop = document.getElementById('btnStudioSaveTop');
  if (btnSaveTop) btnSaveTop.addEventListener('click', saveStudioPermissions);

  const btnSaveBottom = document.getElementById('btnStudioSaveBottom');
  if (btnSaveBottom) btnSaveBottom.addEventListener('click', saveStudioPermissions);

  // =========================================================
  // 8. TAB 2: WORKFORCE USER ROLE ASSIGNMENTS
  // =========================================================
  const loadUsers = async () => {
    try {
      const res = await fetch(`${API_BASE}/auth/users`);
      if (res.ok) {
        const json = await res.json();
        usersCache = json.data || [];
        populateAssignUserDropdowns();
        renderUserRoleAssignmentsTable(usersCache);
        
        const countBadge = document.getElementById('usersTableCountBadge');
        if (countBadge) countBadge.textContent = `${usersCache.length} Accounts`;

        const uPill = document.getElementById('tabUserCountPill');
        if (uPill) uPill.textContent = `${usersCache.length} Users`;
      }
    } catch (e) {
      console.error('Error loading users:', e);
    }
  };

  const renderUserRoleAssignmentsTable = (users) => {
    const tbody = document.getElementById('userRoleAssignmentsTableBody');
    if (!tbody) return;

    if (!users || users.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="6" style="text-align:center; padding:30px; color:#94a3b8;">
            No user accounts found matching your query.
          </td>
        </tr>`;
      return;
    }

    tbody.innerHTML = users.map(user => {
      const roleName = user.role || 'ROLE_USER';
      const roleObj = rolesCache.find(r => r.name === roleName);
      const displayName = user.displayRole || (roleObj ? roleObj.displayName : (user.isAdmin ? 'Administrator' : 'Standard Employee'));
      const permCount = (user.permissions && user.permissions.length > 0)
        ? user.permissions.length 
        : (roleObj ? (roleObj.permissions || []).length : (user.isAdmin ? TOTAL_SYSTEM_PERMS : 4));

      let badgeBg = '#e0f2fe';
      let badgeColor = '#0284c7';
      let icon = '👤';

      if (user.isAdmin || roleName === 'ROLE_ADMIN') {
        badgeBg = '#fee2e2';
        badgeColor = '#dc2626';
        icon = '👑';
      } else if (roleName === 'ROLE_HR_MANAGER') {
        badgeBg = '#ede9fe';
        badgeColor = '#7c3aed';
        icon = '💼';
      } else if (roleName === 'ROLE_MANAGER') {
        badgeBg = '#e0e7ff';
        badgeColor = '#4338ca';
        icon = '🏢';
      } else if (roleName === 'ROLE_VIEWER') {
        badgeBg = '#f1f5f9';
        badgeColor = '#475569';
        icon = '👁️';
      }

      return `
        <tr style="border-bottom:1px solid #f1f5f9;">
          <td style="padding:14px 20px;">
            <div style="display:flex; align-items:center; gap:10px;">
              <div style="width:36px; height:36px; border-radius:50%; background:${badgeBg}; color:${badgeColor}; display:flex; align-items:center; justify-content:center; font-weight:800; font-size:14px;">
                ${escapeHtml((user.fullName || user.email || 'U').charAt(0).toUpperCase())}
              </div>
              <div>
                <div style="font-weight:700; color:#0f172a; font-size:13.5px;">${escapeHtml(user.fullName || 'User')}</div>
                <div style="font-size:11.5px; color:#64748b;">User ID: #${user.id}</div>
              </div>
            </div>
          </td>
          <td style="padding:14px 20px;">
            <code style="font-size:12.5px; color:#334155; background:#f8fafc; padding:3px 7px; border-radius:4px; border:1px solid #e2e8f0; font-family:'JetBrains Mono', monospace;">
              ${escapeHtml(user.email)}
            </code>
          </td>
          <td style="padding:14px 20px; font-size:13px; color:#475569;">
            ${escapeHtml(user.department || 'General')}
          </td>
          <td style="padding:14px 20px; text-align:center;">
            <span style="display:inline-flex; align-items:center; gap:5px; background:${badgeBg}; color:${badgeColor}; font-weight:700; font-size:11.5px; padding:3px 10px; border-radius:12px; text-transform:uppercase;">
              <span>${icon}</span> ${escapeHtml(displayName)}
            </span>
          </td>
          <td style="padding:14px 20px; text-align:center;">
            <span style="background:#f1f5f9; color:#334155; font-weight:700; padding:2px 8px; border-radius:10px; font-size:12px;">
              ${permCount} Granted
            </span>
          </td>
          <td style="padding:14px 20px; text-align:right;">
            <div style="display:inline-flex; gap:6px; align-items:center;">
              <button type="button" class="btn btn-outline" onclick="window.openAssignUserModalForEmail('${escapeHtml(user.email)}', '${escapeHtml(roleName)}')" style="padding:5px 12px; font-size:12px; border-color:#0284c7; color:#0284c7; font-weight:700; border-radius:6px; cursor:pointer;" title="Change this user's assigned role">
                ✏️ Change Role
              </button>
              <button type="button" class="btn btn-outline" onclick="window.jumpToRoleInStudio('${escapeHtml(roleName)}')" style="padding:5px 10px; font-size:12px; border-color:#cbd5e1; color:#475569; border-radius:6px; cursor:pointer;" title="View or edit the permission policy for this role in Studio">
                🛡️ View Policy
              </button>
            </div>
          </td>
        </tr>`;
    }).join('');
  };

  window.openAssignUserModalForEmail = (email, currentRole) => {
    loadUsers().then(() => {
      const userSelect = document.getElementById('assignSelectUser');
      const roleSelect = document.getElementById('assignSelectRole');
      if (userSelect && email) userSelect.value = email;
      if (roleSelect && currentRole) roleSelect.value = currentRole;
      const modal = document.getElementById('assignUserRoleModalBackdrop');
      if (modal) modal.classList.add('open');
    });
  };

  window.jumpToRoleInStudio = (roleName) => {
    const role = rolesCache.find(r => r.name.toUpperCase() === roleName.toUpperCase());
    if (role) {
      window.switchWorkspaceTab('studio');
      window.selectRoleInStudio(role.id);
    } else {
      showToast(`Role '${roleName}' not found in active directory.`, 'error');
    }
  };

  const searchUserInput = document.getElementById('searchUserInput');
  const filterUserRoleSelect = document.getElementById('filterUserRoleSelect');

  const filterUsersList = () => {
    const q = searchUserInput ? searchUserInput.value.toLowerCase().trim() : '';
    const selectedRole = filterUserRoleSelect ? filterUserRoleSelect.value : '';

    const filtered = usersCache.filter(u => {
      const matchQ = !q || 
        (u.fullName && u.fullName.toLowerCase().includes(q)) || 
        (u.email && u.email.toLowerCase().includes(q)) || 
        (u.department && u.department.toLowerCase().includes(q));
      const matchRole = !selectedRole || (u.role && u.role.toUpperCase() === selectedRole.toUpperCase());
      return matchQ && matchRole;
    });

    renderUserRoleAssignmentsTable(filtered);
  };

  if (searchUserInput) searchUserInput.addEventListener('input', filterUsersList);
  if (filterUserRoleSelect) filterUserRoleSelect.addEventListener('change', filterUsersList);

  const populateAssignUserDropdowns = () => {
    const userSelect = document.getElementById('assignSelectUser');
    const roleSelect = document.getElementById('assignSelectRole');

    if (userSelect) {
      userSelect.innerHTML = `<option value="">-- Choose User Account --</option>` + 
        usersCache.map(u => `
          <option value="${escapeHtml(u.email)}">${escapeHtml(u.fullName)} (${escapeHtml(u.email)}) - [Current: ${escapeHtml(u.displayRole)}]</option>
        `).join('');
    }

    if (roleSelect) {
      roleSelect.innerHTML = `<option value="">-- Choose Role Policy --</option>` + 
        rolesCache.filter(r => r.active).map(r => `
          <option value="${escapeHtml(r.name)}">${escapeHtml(r.displayName)} (${escapeHtml(r.name)})</option>
        `).join('');
    }
  };

  // =========================================================
  // 9. TAB 3: SECURITY AUDIT LOGS
  // =========================================================
  const loadAuditLogs = async () => {
    try {
      const res = await fetch(`${API_BASE}/roles/audit-logs`);
      if (res.ok) {
        const json = await res.json();
        auditLogsCache = json.data || [];
        renderAuditLogs(auditLogsCache);
      }
    } catch (e) {
      console.error('Error loading audit logs:', e);
    }
  };

  const renderAuditLogs = (logs) => {
    const tbody = document.getElementById('auditLogTableBody');
    if (!tbody) return;

    if (!logs || logs.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="5" style="text-align:center; padding:30px; color:#94a3b8;">
            No security audit records logged yet.
          </td>
        </tr>`;
      return;
    }

    tbody.innerHTML = logs.map(l => {
      const dateStr = l.createdAt ? new Date(l.createdAt).toLocaleString() : 'Just now';
      const isCreate = l.action && l.action.includes('CREATE');
      const isDelete = l.action && l.action.includes('DELETE');

      const actionBadge = isCreate 
        ? `<span class="badge-custom">${escapeHtml(l.action)}</span>`
        : isDelete 
          ? `<span style="background:#fee2e2; color:#dc2626; border:1px solid #fecaca; font-size:10.5px; font-weight:700; padding:2px 6px; border-radius:4px; text-transform:uppercase;">${escapeHtml(l.action)}</span>`
          : `<span class="badge-system">${escapeHtml(l.action)}</span>`;

      return `
        <tr style="border-bottom:1px solid #f1f5f9;">
          <td style="padding:12px 18px; font-size:12.5px; color:#64748b; white-space:nowrap;">${dateStr}</td>
          <td style="padding:12px 18px;">${actionBadge}</td>
          <td style="padding:12px 18px; font-weight:700; font-size:13px; color:#0f172a;">
            ${escapeHtml(l.targetRole || l.targetUser || '—')}
          </td>
          <td style="padding:12px 18px; font-size:13px; color:#475569;">${escapeHtml(l.performedBy || 'system')}</td>
          <td style="padding:12px 18px; font-size:13px; color:#334155;">${escapeHtml(l.details || '—')}</td>
        </tr>`;
    }).join('');
  };

  const searchAuditInput = document.getElementById('searchAuditInput');
  if (searchAuditInput) {
    searchAuditInput.addEventListener('input', (e) => {
      const q = e.target.value.toLowerCase().trim();
      const filtered = auditLogsCache.filter(l => 
        (l.action && l.action.toLowerCase().includes(q)) ||
        (l.targetRole && l.targetRole.toLowerCase().includes(q)) ||
        (l.targetUser && l.targetUser.toLowerCase().includes(q)) ||
        (l.performedBy && l.performedBy.toLowerCase().includes(q)) ||
        (l.details && l.details.toLowerCase().includes(q))
      );
      renderAuditLogs(filtered);
    });
  }

  const btnRefreshAudit = document.getElementById('btnRefreshAudit');
  if (btnRefreshAudit) {
    btnRefreshAudit.addEventListener('click', async () => {
      await loadAuditLogs();
      showToast('Audit trail refreshed.', 'info');
    });
  }

  // =========================================================
  // 10. MODALS: CREATE ROLE, EDIT ROLE, ASSIGN ROLE
  // =========================================================
  // Modal 1: Create Role
  const createRoleModal = document.getElementById('createRoleModalBackdrop');
  const btnOpenCreate = document.getElementById('btnOpenCreateRoleModal');
  const btnCloseCreate = document.getElementById('btnCloseCreateRoleModal');
  const btnCancelCreate = document.getElementById('btnCancelCreateRole');
  const createRoleForm = document.getElementById('createRoleForm');

  if (btnOpenCreate) {
    btnOpenCreate.addEventListener('click', () => {
      createRoleForm.reset();
      createRoleModal.classList.add('open');
    });
  }
  const closeCreateModal = () => createRoleModal.classList.remove('open');
  if (btnCloseCreate) btnCloseCreate.addEventListener('click', closeCreateModal);
  if (btnCancelCreate) btnCancelCreate.addEventListener('click', closeCreateModal);

  if (createRoleForm) {
    createRoleForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const displayName = document.getElementById('newRoleDisplayName').value.trim();
      const name = document.getElementById('newRoleIdentifier').value.trim();
      const description = document.getElementById('newRoleDescription').value.trim();
      const active = document.getElementById('newRoleActive').checked;

      const submitBtn = document.getElementById('btnSubmitCreateRole');
      submitBtn.disabled = true;
      submitBtn.textContent = 'Creating...';

      try {
        const res = await fetch(`${API_BASE}/roles`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ displayName, name, description, active })
        });
        const json = await res.json();
        if (res.ok) {
          showToast(`Custom role '${json.data.displayName}' created!`, 'success');
          closeCreateModal();
          await loadRoles();
          await loadDashboardStats();
          await loadAuditLogs();
          selectRoleInStudio(json.data.id);
        } else {
          showToast(json.message || 'Failed to create role.', 'error');
        }
      } catch (err) {
        showToast('Server error while creating role.', 'error');
      } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Create Role';
      }
    });
  }

  // Modal 2: Edit Role
  const editRoleModal = document.getElementById('editRoleModalBackdrop');
  const btnCloseEdit = document.getElementById('btnCloseEditRoleModal');
  const btnCancelEdit = document.getElementById('btnCancelEditRole');
  const editRoleForm = document.getElementById('editRoleForm');

  window.openEditRoleModal = (roleId) => {
    const role = rolesCache.find(r => r.id === roleId);
    if (!role) return;

    document.getElementById('editRoleId').value = role.id;
    document.getElementById('editRoleIdentifier').value = role.name;
    document.getElementById('editRoleDisplayName').value = role.displayName;
    document.getElementById('editRoleDescription').value = role.description || '';
    
    const activeCheckbox = document.getElementById('editRoleActive');
    activeCheckbox.checked = role.active;

    if (role.name === 'ROLE_ADMIN') {
      activeCheckbox.disabled = true;
    } else {
      activeCheckbox.disabled = false;
    }

    editRoleModal.classList.add('open');
  };

  const closeEditModal = () => editRoleModal.classList.remove('open');
  if (btnCloseEdit) btnCloseEdit.addEventListener('click', closeEditModal);
  if (btnCancelEdit) btnCancelEdit.addEventListener('click', closeEditModal);

  if (editRoleForm) {
    editRoleForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const roleId = document.getElementById('editRoleId').value;
      const displayName = document.getElementById('editRoleDisplayName').value.trim();
      const description = document.getElementById('editRoleDescription').value.trim();
      const active = document.getElementById('editRoleActive').checked;

      const submitBtn = document.getElementById('btnSubmitEditRole');
      submitBtn.disabled = true;
      submitBtn.textContent = 'Saving...';

      try {
        const res = await fetch(`${API_BASE}/roles/${roleId}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ displayName, description, active })
        });
        const json = await res.json();
        if (res.ok) {
          showToast(`Role '${json.data.displayName}' updated!`, 'success');
          closeEditModal();
          await loadRoles();
          await loadAuditLogs();
        } else {
          showToast(json.message || 'Failed to update role.', 'error');
        }
      } catch (err) {
        showToast('Server error while updating role.', 'error');
      } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Save Changes';
      }
    });
  }

  // Modal 3: Assign Role to User
  const assignUserModal = document.getElementById('assignUserRoleModalBackdrop');
  const btnOpenAssign = document.getElementById('btnOpenAssignUserModal');
  const btnCloseAssign = document.getElementById('btnCloseAssignUserModal');
  const btnCancelAssign = document.getElementById('btnCancelAssignUser');
  const assignUserForm = document.getElementById('assignUserRoleForm');

  if (btnOpenAssign) {
    btnOpenAssign.addEventListener('click', async () => {
      await loadUsers();
      populateAssignUserDropdowns();
      assignUserForm.reset();
      assignUserModal.classList.add('open');
    });
  }

  const closeAssignModal = () => assignUserModal.classList.remove('open');
  if (btnCloseAssign) btnCloseAssign.addEventListener('click', closeAssignModal);
  if (btnCancelAssign) btnCancelAssign.addEventListener('click', closeAssignModal);

  if (assignUserForm) {
    assignUserForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const userEmail = document.getElementById('assignSelectUser').value;
      const roleName = document.getElementById('assignSelectRole').value;

      if (!userEmail || !roleName) {
        showToast('Please select both a user and a role policy.', 'error');
        return;
      }

      const submitBtn = document.getElementById('btnSubmitAssignUser');
      submitBtn.disabled = true;
      submitBtn.textContent = 'Assigning...';

      try {
        const res = await fetch(`${API_BASE}/roles/assign-user`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ userEmail, roleName })
        });
        const json = await res.json();
        if (res.ok) {
          showToast(`Role assigned successfully to ${json.data.fullName}!`, 'success');
          closeAssignModal();
          await Promise.all([
            loadRoles(),
            loadDashboardStats(),
            loadUsers(),
            loadAuditLogs()
          ]);
        } else {
          showToast(json.message || 'Failed to assign role.', 'error');
        }
      } catch (err) {
        showToast('Server error while assigning role.', 'error');
      } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Assign Role';
      }
    });
  }

  // Toggle Role Status (Activate / Deactivate)
  window.toggleRoleStatus = async (roleId, targetActive) => {
    const role = rolesCache.find(r => r.id === roleId);
    if (!role) return;

    if (role.name === 'ROLE_ADMIN' && !targetActive) {
      showToast('Master Administrator role cannot be deactivated.', 'error');
      return;
    }

    const actionWord = targetActive ? 'activate' : 'deactivate';
    if (!confirm(`Are you sure you want to ${actionWord} the role '${role.displayName}'?`)) {
      return;
    }

    try {
      const res = await fetch(`${API_BASE}/roles/${roleId}/status`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ active: targetActive })
      });
      const json = await res.json();
      if (res.ok) {
        showToast(json.message || `Role status updated.`, 'success');
        await loadRoles();
        await loadAuditLogs();
      } else {
        showToast(json.message || 'Failed to update status.', 'error');
      }
    } catch (e) {
      showToast('Error updating role status.', 'error');
    }
  };

  // Delete Role
  window.deleteRole = async (roleId) => {
    const role = rolesCache.find(r => r.id === roleId);
    if (!role) return;

    if (role.systemRole) {
      showToast('System roles cannot be deleted.', 'error');
      return;
    }

    if ((role.assignedUsersCount || 0) > 0) {
      showToast(`Cannot delete role with ${role.assignedUsersCount} assigned users.`, 'error');
      return;
    }

    if (!confirm(`Are you sure you want to permanently delete custom role '${role.displayName}'?`)) {
      return;
    }

    try {
      const res = await fetch(`${API_BASE}/roles/${roleId}`, {
        method: 'DELETE'
      });
      const json = await res.json();
      if (res.ok) {
        showToast(`Role '${role.displayName}' deleted.`, 'success');
        activeRoleId = null;
        await Promise.all([
          loadRoles(),
          loadDashboardStats(),
          loadAuditLogs()
        ]);
      } else {
        showToast(json.message || 'Failed to delete role.', 'error');
      }
    } catch (e) {
      showToast('Error deleting role.', 'error');
    }
  };

  // Global Refresh Button
  const btnRefresh = document.getElementById('btnRefreshData');
  if (btnRefresh) {
    btnRefresh.addEventListener('click', async () => {
      btnRefresh.style.transform = 'rotate(180deg)';
      btnRefresh.style.transition = 'transform 0.4s ease';
      await Promise.all([
        loadDashboardStats(),
        loadRoles(),
        loadUsers(),
        loadAuditLogs()
      ]);
      setTimeout(() => {
        btnRefresh.style.transform = 'none';
      }, 400);
      showToast('Roles, permissions, and accounts refreshed.', 'info');
    });
  }

  // Escape HTML Helper
  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // Initial Data Load
  await Promise.all([
    loadDashboardStats(),
    loadRoles(),
    loadUsers(),
    loadAuditLogs()
  ]);

});
