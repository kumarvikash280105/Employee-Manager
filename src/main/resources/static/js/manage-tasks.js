/**
 * Manage Tasks & Team Deliverables
 * EmpManage Enterprise Platform
 */

let currentUser = null;
let allTeamTasks = [];
let availableAssignees = [];
let currentFilterStatus = 'ALL';
let currentFilterPriority = 'ALL';
let currentFilterDept = 'ALL';
let currentSearchQuery = '';
let searchTimeout = null;

document.addEventListener('DOMContentLoaded', () => {
  initDashboard();
});

async function initDashboard() {
  try {
    const res = await fetch('/api/auth/me', { credentials: 'include' });
    if (!res.ok) {
      window.location.href = 'index.html?error=unauthorized';
      return;
    }
    const json = await res.json();
    if (json.success && json.data) {
      currentUser = json.data;

      // Ensure user has management authority
      const isAdmin = currentUser.role === 'ROLE_ADMIN' || currentUser.isAdmin === true;
      const isManager = currentUser.role === 'ROLE_MANAGER' || currentUser.role === 'ROLE_HR_MANAGER';
      const hasTaskPerms = currentUser.permissions && (
        currentUser.permissions.includes('TASK_VIEW_ALL') ||
        currentUser.permissions.includes('TASK_CREATE') ||
        currentUser.permissions.includes('TASK_REVIEW')
      );

      if (!isAdmin && !isManager && !hasTaskPerms) {
        window.location.href = 'employee-tasks.html?error=forbidden';
        return;
      }

      updateHeaderUI();
      await Promise.all([
        loadDepartments(),
        loadAssigneesDirectory(),
        loadAnalytics(),
        loadAllTasks()
      ]);
    } else {
      window.location.href = 'index.html?error=unauthorized';
    }
  } catch (err) {
    console.error('Initialization error:', err);
    window.location.href = 'index.html?error=unauthorized';
  }
}

function updateHeaderUI() {
  if (!currentUser) return;
  const nameEl = document.getElementById('headerUserName');
  const avatarEl = document.getElementById('headerUserAvatar');

  const fullName = currentUser.fullName || currentUser.email;
  if (nameEl) nameEl.textContent = fullName;
  if (avatarEl) {
    const initials = fullName
      .split(' ')
      .map(n => n[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();
    avatarEl.textContent = initials || 'M';
  }
}

async function loadDepartments() {
  try {
    const res = await fetch('/api/departments', { credentials: 'include' });
    const json = await res.json();
    if (json.success && Array.isArray(json.data)) {
      const select = document.getElementById('taskDeptSelect');
      if (select) {
        select.innerHTML = '<option value="ALL">All Departments</option>' +
          json.data.map(d => `<option value="${escapeHtml(d.name)}">${escapeHtml(d.name)}</option>`).join('');
      }
    }
  } catch (e) {
    console.warn('Could not load departments:', e);
  }
}

async function loadAssigneesDirectory() {
  try {
    const assigneesMap = new Map();

    // 1. Fetch from employees
    try {
      const resEmp = await fetch('/api/employees', { credentials: 'include' });
      const jsonEmp = await resEmp.json();
      const list = jsonEmp.data && jsonEmp.data.employees ? jsonEmp.data.employees : (Array.isArray(jsonEmp.data) ? jsonEmp.data : []);
      list.forEach(e => {
        if (e.email) {
          assigneesMap.set(e.email.toLowerCase(), {
            name: e.name || e.email,
            email: e.email.toLowerCase(),
            department: e.departmentName || (e.department ? e.department.name : 'General')
          });
        }
      });
    } catch (e) {}

    // 2. Fetch from users
    try {
      const resUsers = await fetch('/api/auth/users', { credentials: 'include' });
      if (resUsers.ok) {
        const jsonUsers = await resUsers.json();
        if (jsonUsers.success && Array.isArray(jsonUsers.data)) {
          jsonUsers.data.forEach(u => {
            if (u.email && !assigneesMap.has(u.email.toLowerCase())) {
              assigneesMap.set(u.email.toLowerCase(), {
                name: u.fullName || u.email,
                email: u.email.toLowerCase(),
                department: u.department || 'General'
              });
            }
          });
        }
      }
    } catch (e) {}

    availableAssignees = Array.from(assigneesMap.values()).sort((a, b) => a.name.localeCompare(b.name));

    // Populate create task modal select
    populateAssigneeSelect('newTaskAssignee', availableAssignees);
    populateAssigneeSelect('editTaskAssignee', availableAssignees);
  } catch (err) {
    console.warn('Assignee load error:', err);
  }
}

function populateAssigneeSelect(selectId, list) {
  const select = document.getElementById(selectId);
  if (!select) return;
  select.innerHTML = '<option value="">-- Select Assignee --</option>' +
    list.map(a => `<option value="${escapeHtml(a.email)}">${escapeHtml(a.name)} (${escapeHtml(a.email)}) - [${escapeHtml(a.department)}]</option>`).join('');
}

async function loadAnalytics() {
  try {
    const res = await fetch('/api/tasks/analytics', { credentials: 'include' });
    const json = await res.json();
    if (json.success && json.data) {
      const a = json.data;
      safeSetText('statTotal', a.totalTasks || 0);
      safeSetText('statToDo', a.toDoTasks || 0);
      safeSetText('statInProgress', a.inProgressTasks || 0);
      safeSetText('statInReview', a.inReviewTasks || 0);
      safeSetText('statCompleted', a.completedTasks || 0);
      safeSetText('statOverdue', a.overdueTasks || 0);
      safeSetText('statCompletionRate', `${a.completionRate || 0}%`);
    }
  } catch (err) {
    console.warn('Analytics fetch error:', err);
  }
}

async function loadAllTasks() {
  const tbody = document.getElementById('tasksTableBody');
  try {
    let url = '/api/tasks/all';
    const params = new URLSearchParams();
    if (currentFilterStatus !== 'ALL' && currentFilterStatus !== 'OVERDUE') {
      params.append('status', currentFilterStatus);
    }
    if (currentFilterPriority !== 'ALL') {
      params.append('priority', currentFilterPriority);
    }
    if (currentFilterDept !== 'ALL') {
      params.append('department', currentFilterDept);
    }
    if (currentSearchQuery.trim()) {
      params.append('query', currentSearchQuery.trim());
    }
    if (params.toString()) {
      url += '?' + params.toString();
    }

    const res = await fetch(url, { credentials: 'include' });
    const json = await res.json();

    if (json.success && Array.isArray(json.data)) {
      allTeamTasks = json.data;
      renderTable(filterTasksLocally(allTeamTasks));
    } else {
      showToast(json.message || 'Failed to fetch team tasks', 'error');
    }
  } catch (err) {
    if (tbody) {
      tbody.innerHTML = `
        <tr>
          <td colspan="8">
            <div class="empty-state">
              <div class="empty-state-icon">⚠️</div>
              <div class="empty-state-title">Unable to fetch tasks</div>
              <div class="empty-state-desc">${escapeHtml(err.message)}</div>
            </div>
          </td>
        </tr>
      `;
    }
  }
}

function filterTasksLocally(tasks) {
  return tasks.filter(t => {
    if (currentFilterStatus === 'OVERDUE' && !t.overdue) return false;
    return true;
  });
}

function renderTable(tasks) {
  const tbody = document.getElementById('tasksTableBody');
  if (!tbody) return;

  if (!tasks || tasks.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="8">
          <div class="empty-state">
            <div class="empty-state-icon">📂</div>
            <div class="empty-state-title">No tasks found</div>
            <div class="empty-state-desc">Try clearing or adjusting your search filters, or create a new task deliverable.</div>
          </div>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = tasks.map(t => {
    const priorityClass = `badge-priority-${(t.priority || 'medium').toLowerCase()}`;
    const statusClass = `badge-status-${(t.status || 'to_do').toLowerCase().replace('_', '-')}`;
    const isInReview = t.status === 'IN_REVIEW';

    let fillClass = '#0284c7';
    if (t.progress >= 100) fillClass = '#10b981';
    else if (t.progress >= 70) fillClass = '#8b5cf6';
    else if (t.progress >= 30) fillClass = '#f59e0b';

    return `
      <tr class="${isInReview ? 'row-in-review' : ''}">
        <td>
          <div style="font-weight: 700; color: #0f172a; margin-bottom: 2px;">
            ${escapeHtml(t.title)}
          </div>
          <div style="font-size: 11.5px; color: #64748b; font-family: 'JetBrains Mono', monospace;">
            #${t.id} ${t.description ? '&bull; ' + escapeHtml(t.description.substring(0, 50)) + (t.description.length > 50 ? '...' : '') : ''}
          </div>
        </td>
        <td>
          <div style="font-weight: 600; color: #0f172a;">${escapeHtml(t.assignedToName || t.assignedToEmail)}</div>
          <div style="font-size: 12px; color: #64748b;">
            ${escapeHtml(t.assignedToDepartment || 'General')} &bull; ${escapeHtml(t.assignedToEmail)}
          </div>
        </td>
        <td>
          <span class="task-badge ${priorityClass}">⚡ ${t.priority || 'MEDIUM'}</span>
        </td>
        <td>
          <span class="task-badge ${statusClass}">${formatStatus(t.status)}</span>
          ${t.overdue ? '<br><span class="task-badge badge-overdue" style="margin-top:4px;">⚠️ OVERDUE</span>' : ''}
        </td>
        <td>
          <div style="font-weight: 700; font-size: 12px; font-family: 'JetBrains Mono', monospace; color: #0f172a;">
            ${t.progress}%
          </div>
          <div class="mini-progress-track">
            <div class="mini-progress-fill" style="width: ${t.progress}%; background: ${fillClass};"></div>
          </div>
        </td>
        <td>
          <span style="font-family: 'JetBrains Mono', monospace; font-size: 12px; ${t.overdue ? 'color:#ef4444; font-weight:700;' : ''}">
            ${t.formattedDueDate || '--'}
          </span>
        </td>
        <td>
          <span style="font-size: 12.5px; color: #475569;">${escapeHtml(t.createdByName || 'System')}</span>
        </td>
        <td style="text-align: right;">
          <div class="table-actions" style="justify-content: flex-end;">
            ${isInReview ? `
              <button type="button" class="btn-tbl-action btn-tbl-review" onclick="openReviewModal(${t.id})">
                <span>🔍 Review</span>
              </button>
            ` : `
              <button type="button" class="btn-tbl-action btn-tbl-review" style="background:#f8fafc; border-color:#e2e8f0; color:#64748b;" onclick="openReviewModal(${t.id})">
                <span>Review</span>
              </button>
            `}
            <button type="button" class="btn-tbl-action btn-tbl-edit" onclick="openEditModal(${t.id})" title="Edit Task">
              <span>✏️</span>
            </button>
            <button type="button" class="btn-tbl-action btn-tbl-history" onclick="openHistoryModal(${t.id})" title="Audit Log">
              <span>📜</span>
            </button>
            <button type="button" class="btn-tbl-action btn-tbl-delete" onclick="executeDeleteTask(${t.id}, '${escapeJsString(t.title)}')" title="Delete Task">
              <span>🗑️</span>
            </button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function formatStatus(status) {
  if (!status) return 'To Do';
  switch (status) {
    case 'TO_DO': return 'To Do';
    case 'IN_PROGRESS': return 'In Progress';
    case 'IN_REVIEW': return 'Under Review';
    case 'COMPLETED': return 'Completed';
    case 'CANCELLED': return 'Cancelled';
    default: return status;
  }
}

// Modal 1: Create Task
function openCreateTaskModal() {
  document.getElementById('createTaskForm').reset();
  const today = new Date().toISOString().split('T')[0];
  document.getElementById('newTaskStartDate').value = today;
  openModal('createTaskModal');
}

async function executeCreateTask(e) {
  e.preventDefault();
  const title = document.getElementById('newTaskTitle').value.trim();
  const assignedToEmail = document.getElementById('newTaskAssignee').value;
  const priority = document.getElementById('newTaskPriority').value;
  const startDate = document.getElementById('newTaskStartDate').value || null;
  const dueDate = document.getElementById('newTaskDueDate').value || null;
  const description = document.getElementById('newTaskDescription').value.trim();

  if (!title) {
    showToast('Task title is required.', 'error');
    return;
  }
  if (!assignedToEmail) {
    showToast('Please select an employee to assign this task.', 'error');
    return;
  }
  if (startDate && dueDate && dueDate < startDate) {
    showToast('Due date cannot be before start date.', 'error');
    return;
  }

  const btn = document.getElementById('btnSubmitCreateTask');
  btn.disabled = true;
  btn.textContent = 'Assigning...';

  try {
    const res = await fetch('/api/tasks', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        title,
        description,
        assignedToEmail,
        priority,
        startDate,
        dueDate
      })
    });
    const json = await res.json();
    if (json.success) {
      showToast('Task created and assigned successfully!', 'success');
      closeModal('createTaskModal');
      await Promise.all([loadAnalytics(), loadAllTasks()]);
    } else {
      showToast(json.message || 'Failed to create task', 'error');
    }
  } catch (err) {
    showToast('Network error while creating task', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Create & Assign';
  }
}

// Modal 2: Review Task
let selectedReviewAction = 'APPROVE';

function openReviewModal(taskId) {
  const task = allTeamTasks.find(t => t.id === taskId);
  if (!task) return;

  document.getElementById('reviewModalTaskId').value = task.id;
  document.getElementById('reviewModalTaskTitle').textContent = task.title;
  document.getElementById('reviewModalAssignee').textContent = `${task.assignedToName} (${task.assignedToEmail})`;
  document.getElementById('reviewFeedback').value = '';

  setReviewDecision('APPROVE');
  openModal('reviewTaskModal');
}

function setReviewDecision(action) {
  selectedReviewAction = action;
  const cardApprove = document.getElementById('cardDecisionApprove');
  const cardChanges = document.getElementById('cardDecisionChanges');
  const radioApprove = document.getElementById('radioApprove');
  const radioChanges = document.getElementById('radioChanges');

  if (action === 'APPROVE') {
    if (cardApprove) cardApprove.className = 'decision-card selected-approve';
    if (cardChanges) cardChanges.className = 'decision-card';
    if (radioApprove) radioApprove.checked = true;
  } else {
    if (cardApprove) cardApprove.className = 'decision-card';
    if (cardChanges) cardChanges.className = 'decision-card selected-changes';
    if (radioChanges) radioChanges.checked = true;
  }
}

async function executeTaskReview(e) {
  e.preventDefault();
  const taskId = document.getElementById('reviewModalTaskId').value;
  const feedback = document.getElementById('reviewFeedback').value.trim();

  const btn = document.getElementById('btnSubmitReview');
  btn.disabled = true;
  btn.textContent = 'Submitting...';

  try {
    const res = await fetch(`/api/tasks/${taskId}/review`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        action: selectedReviewAction,
        feedback: feedback
      })
    });
    const json = await res.json();
    if (json.success) {
      showToast(json.message || 'Task review submitted successfully!', 'success');
      closeModal('reviewTaskModal');
      await Promise.all([loadAnalytics(), loadAllTasks()]);
    } else {
      showToast(json.message || 'Failed to submit review', 'error');
    }
  } catch (err) {
    showToast('Network error while reviewing task', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Submit Review Decision';
  }
}

// Modal 3: Edit Task
function openEditModal(taskId) {
  const task = allTeamTasks.find(t => t.id === taskId);
  if (!task) return;

  document.getElementById('editModalTaskId').value = task.id;
  document.getElementById('editTaskTitle').value = task.title || '';
  document.getElementById('editTaskPriority').value = task.priority || 'MEDIUM';
  document.getElementById('editTaskStatus').value = task.status || 'TO_DO';
  document.getElementById('editTaskProgress').value = task.progress || 0;
  document.getElementById('editTaskDueDate').value = task.dueDate || '';
  document.getElementById('editTaskAssignee').value = task.assignedToEmail || '';
  document.getElementById('editTaskDescription').value = task.description || '';

  openModal('editTaskModal');
}

async function executeEditTask(e) {
  e.preventDefault();
  const taskId = document.getElementById('editModalTaskId').value;
  const title = document.getElementById('editTaskTitle').value.trim();
  const priority = document.getElementById('editTaskPriority').value;
  const status = document.getElementById('editTaskStatus').value;
  const progress = parseInt(document.getElementById('editTaskProgress').value, 10);
  const dueDate = document.getElementById('editTaskDueDate').value || null;
  const assignedToEmail = document.getElementById('editTaskAssignee').value;
  const description = document.getElementById('editTaskDescription').value.trim();

  const btn = document.getElementById('btnSubmitEdit');
  btn.disabled = true;
  btn.textContent = 'Saving...';

  try {
    const res = await fetch(`/api/tasks/${taskId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        title,
        priority,
        status,
        progress,
        dueDate,
        assignedToEmail,
        description
      })
    });
    const json = await res.json();
    if (json.success) {
      showToast('Task updated successfully!', 'success');
      closeModal('editTaskModal');
      await Promise.all([loadAnalytics(), loadAllTasks()]);
    } else {
      showToast(json.message || 'Failed to update task', 'error');
    }
  } catch (err) {
    showToast('Network error while updating task', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Save Changes';
  }
}

// Modal 4: History / Audit Log
async function openHistoryModal(taskId) {
  const task = allTeamTasks.find(t => t.id === taskId);
  const overviewEl = document.getElementById('historyTaskOverview');
  const container = document.getElementById('historyTimelineContainer');

  if (overviewEl && task) {
    overviewEl.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:8px;">
        <h4 style="margin:0; font-size:16px; font-weight:800; color:#0f172a;">${escapeHtml(task.title)}</h4>
        <span class="task-badge badge-status-${(task.status || 'to_do').toLowerCase().replace('_', '-')}">${formatStatus(task.status)}</span>
      </div>
      <div style="font-size:12.5px; color:#64748b;">
        Assignee: <strong>${escapeHtml(task.assignedToName)}</strong> (${escapeHtml(task.assignedToEmail)})
      </div>
    `;
  }

  if (container) {
    container.innerHTML = '<div style="color:#64748b; font-size:13px;">Loading audit logs...</div>';
  }
  openModal('taskHistoryModal');

  try {
    const res = await fetch(`/api/tasks/${taskId}/history`, { credentials: 'include' });
    const json = await res.json();

    if (json.success && Array.isArray(json.data) && json.data.length > 0) {
      container.innerHTML = `
        <div style="display:flex; flex-direction:column; gap:12px;">
          ${json.data.map(item => `
            <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:10px; padding:12px 14px;">
              <div style="display:flex; justify-content:space-between; font-size:12px; margin-bottom:6px;">
                <span style="font-weight:700; color:#0f172a;">${escapeHtml(item.updaterName || item.updaterEmail)} <span style="font-size:11px; color:#64748b; font-weight:normal;">(${item.updaterRole || 'USER'})</span></span>
                <span style="color:#64748b; font-family:'JetBrains Mono',monospace;">${item.formattedTimestamp || ''}</span>
              </div>
              <div style="display:flex; gap:8px; align-items:center; margin-bottom:6px;">
                <span style="font-size:11px; font-weight:700; color:#0284c7; background:#e0f2fe; padding:2px 6px; border-radius:4px;">
                  Progress: ${item.progressPercentage}%
                </span>
                ${item.newStatus ? `
                  <span style="font-size:11px; font-weight:700; color:#475569; background:#f1f5f9; padding:2px 6px; border-radius:4px;">
                    Status: ${formatStatus(item.newStatus)}
                  </span>
                ` : ''}
              </div>
              <div style="font-size:13px; color:#334155; line-height:1.4;">
                ${escapeHtml(item.workDescription || 'No description.')}
              </div>
            </div>
          `).join('')}
        </div>
      `;
    } else {
      container.innerHTML = '<div style="color:#64748b; font-size:13px; font-style:italic;">No progress updates logged yet.</div>';
    }
  } catch (err) {
    container.innerHTML = '<div style="color:#ef4444; font-size:13px;">Error fetching task history.</div>';
  }
}

// Delete Task
async function executeDeleteTask(taskId, taskTitle) {
  if (!confirm(`Are you sure you want to permanently delete task "${taskTitle}"?`)) {
    return;
  }

  try {
    const res = await fetch(`/api/tasks/${taskId}`, {
      method: 'DELETE',
      credentials: 'include'
    });
    const json = await res.json();
    if (json.success) {
      showToast('Task deleted successfully.', 'success');
      await Promise.all([loadAnalytics(), loadAllTasks()]);
    } else {
      showToast(json.message || 'Failed to delete task', 'error');
    }
  } catch (err) {
    showToast('Network error while deleting task', 'error');
  }
}

// Filters & Search
function setQuickStatusFilter(status) {
  currentFilterStatus = status;

  const cards = document.querySelectorAll('.kpi-card');
  cards.forEach(c => c.classList.remove('active-filter'));

  const cardMap = {
    'ALL': 'cardFilterAll',
    'TO_DO': 'cardFilterToDo',
    'IN_PROGRESS': 'cardFilterInProgress',
    'IN_REVIEW': 'cardFilterInReview',
    'COMPLETED': 'cardFilterCompleted',
    'OVERDUE': 'cardFilterOverdue'
  };

  const activeCardId = cardMap[status];
  if (activeCardId) {
    const el = document.getElementById(activeCardId);
    if (el) el.classList.add('active-filter');
  }

  const select = document.getElementById('taskStatusSelect');
  if (select) select.value = status;

  loadAllTasks();
}

function handleFilterChange() {
  const statusSel = document.getElementById('taskStatusSelect');
  const prioritySel = document.getElementById('taskPrioritySelect');
  const deptSel = document.getElementById('taskDeptSelect');

  if (statusSel) currentFilterStatus = statusSel.value;
  if (prioritySel) currentFilterPriority = prioritySel.value;
  if (deptSel) currentFilterDept = deptSel.value;

  loadAllTasks();
}

function debounceSearch() {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    const searchInput = document.getElementById('taskSearchInput');
    if (searchInput) currentSearchQuery = searchInput.value;
    loadAllTasks();
  }, 300);
}

function resetFilters() {
  currentFilterStatus = 'ALL';
  currentFilterPriority = 'ALL';
  currentFilterDept = 'ALL';
  currentSearchQuery = '';

  const searchInput = document.getElementById('taskSearchInput');
  const statusSel = document.getElementById('taskStatusSelect');
  const prioritySel = document.getElementById('taskPrioritySelect');
  const deptSel = document.getElementById('taskDeptSelect');

  if (searchInput) searchInput.value = '';
  if (statusSel) statusSel.value = 'ALL';
  if (prioritySel) prioritySel.value = 'ALL';
  if (deptSel) deptSel.value = 'ALL';

  setQuickStatusFilter('ALL');
}

// Modal helper
function openModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.add('open');
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.remove('open');
}

// Logout helper
async function handleLogout() {
  try {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
  } catch (e) {}
  localStorage.removeItem('empmanage_session_user');
  window.location.href = 'index.html';
}

function showToast(msg, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = msg;

  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

function safeSetText(id, text) {
  const el = document.getElementById(id);
  if (el) el.textContent = text;
}

function escapeHtml(str) {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function escapeJsString(str) {
  if (!str) return '';
  return str.replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '\\"');
}
