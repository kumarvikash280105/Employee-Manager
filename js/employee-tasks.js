/**
 * Employee Task Management Dashboard
 * EmpManage Enterprise Platform
 */

let currentUser = null;
let allMyTasks = [];
let currentFilterStatus = 'ALL';
let currentFilterPriority = 'ALL';
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
      updateHeaderUI();
      await loadMyTasks();
    } else {
      window.location.href = 'index.html?error=unauthorized';
    }
  } catch (err) {
    console.error('Authentication verification error:', err);
    window.location.href = 'index.html?error=unauthorized';
  }
}

function updateHeaderUI() {
  if (!currentUser) return;

  const nameEl = document.getElementById('headerUserName');
  const avatarEl = document.getElementById('headerUserAvatar');
  const welcomeEl = document.getElementById('heroWelcomeName');

  const fullName = currentUser.fullName || currentUser.email;
  if (nameEl) nameEl.textContent = fullName;
  if (welcomeEl) welcomeEl.textContent = `${fullName}'s Deliverables`;

  if (avatarEl) {
    const initials = fullName
      .split(' ')
      .map(n => n[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();
    avatarEl.textContent = initials || 'U';
  }

  // Check if authorized to view manager/admin pages
  const isAdmin = currentUser.role === 'ROLE_ADMIN' || currentUser.isAdmin === true;
  const isManager = currentUser.role === 'ROLE_MANAGER' || currentUser.role === 'ROLE_HR_MANAGER';
  const hasTaskViewAll = currentUser.permissions && currentUser.permissions.includes('TASK_VIEW_ALL');

  const manageLink = document.getElementById('navManageTasksLink');
  const adminLink = document.getElementById('navAdminPortalLink');
  const mobileManageLink = document.getElementById('mobileNavManageTasksLink');
  const mobileAdminLink = document.getElementById('mobileNavAdminPortalLink');

  if (isAdmin || isManager || hasTaskViewAll) {
    if (manageLink) manageLink.style.display = 'inline-flex';
    if (mobileManageLink) mobileManageLink.style.display = 'inline-flex';
  }
  if (isAdmin) {
    if (adminLink) adminLink.style.display = 'inline-flex';
    if (mobileAdminLink) mobileAdminLink.style.display = 'inline-flex';
  }
}

async function loadMyTasks() {
  const container = document.getElementById('tasksCardsGrid');
  try {
    let url = '/api/tasks/my';
    const params = new URLSearchParams();
    if (currentFilterStatus !== 'ALL' && currentFilterStatus !== 'OVERDUE') {
      params.append('status', currentFilterStatus);
    }
    if (currentFilterPriority !== 'ALL') {
      params.append('priority', currentFilterPriority);
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
      allMyTasks = json.data;
      updateKpis(allMyTasks);
      renderTasks(filterTasksLocally(allMyTasks));
    } else {
      showToast(json.message || 'Failed to fetch tasks', 'error');
    }
  } catch (err) {
    console.error('Error loading tasks:', err);
    if (container) {
      container.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">⚠️</div>
          <div class="empty-state-title">Unable to connect to server</div>
          <div class="empty-state-desc">${escapeHtml(err.message)}</div>
        </div>
      `;
    }
  }
}

function updateKpis(tasks) {
  let total = tasks.length;
  let toDo = 0;
  let inProgress = 0;
  let inReview = 0;
  let completed = 0;
  let overdue = 0;

  tasks.forEach(t => {
    if (t.status === 'TO_DO') toDo++;
    else if (t.status === 'IN_PROGRESS') inProgress++;
    else if (t.status === 'IN_REVIEW') inReview++;
    else if (t.status === 'COMPLETED') completed++;

    if (t.overdue) overdue++;
  });

  safeSetText('statTotalTasks', total);
  safeSetText('statToDoTasks', toDo);
  safeSetText('statInProgressTasks', inProgress);
  safeSetText('statInReviewTasks', inReview);
  safeSetText('statCompletedTasks', completed);
  safeSetText('statOverdueTasks', overdue);
}

function filterTasksLocally(tasks) {
  return tasks.filter(t => {
    if (currentFilterStatus === 'OVERDUE' && !t.overdue) return false;
    return true;
  });
}

function renderTasks(tasks) {
  const container = document.getElementById('tasksCardsGrid');
  if (!container) return;

  if (!tasks || tasks.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon">🎉</div>
        <div class="empty-state-title">No matching tasks found</div>
        <div class="empty-state-desc">You are completely up-to-date or no tasks match your current filter settings.</div>
      </div>
    `;
    return;
  }

  container.innerHTML = tasks.map(task => renderTaskCardHtml(task)).join('');
}

function renderTaskCardHtml(t) {
  const priorityClass = `badge-priority-${(t.priority || 'medium').toLowerCase()}`;
  const statusClass = `badge-status-${(t.status || 'to_do').toLowerCase().replace('_', '-')}`;
  const statusLabel = formatStatus(t.status);

  // Progress fill color class
  let fillClass = 'progress-fill-low';
  if (t.progress >= 100) fillClass = 'progress-fill-done';
  else if (t.progress >= 70) fillClass = 'progress-fill-high';
  else if (t.progress >= 30) fillClass = 'progress-fill-mid';

  const isCompleted = t.status === 'COMPLETED';
  const isCancelled = t.status === 'CANCELLED';
  const isInReview = t.status === 'IN_REVIEW';

  const canUpdateProgress = !isCompleted && !isCancelled;
  const canSubmit = !isCompleted && !isCancelled && !isInReview;

  return `
    <div class="task-card ${t.overdue ? 'is-overdue' : ''}">
      <div>
        <div class="task-card-header">
          <div class="task-badge-row">
            <span class="task-badge ${priorityClass}">⚡ ${t.priority || 'MEDIUM'}</span>
            <span class="task-badge ${statusClass}">${statusLabel}</span>
            ${t.overdue ? '<span class="task-badge badge-overdue">⚠️ OVERDUE</span>' : ''}
          </div>
          <span style="font-size:11.5px; font-weight:700; color:#94a3b8; font-family:'JetBrains Mono',monospace;">#${t.id}</span>
        </div>

        <h3 class="task-card-title">${escapeHtml(t.title)}</h3>
        <p class="task-card-desc">${escapeHtml(t.description || 'No additional description provided.')}</p>

        <!-- Progress Box -->
        <div class="task-progress-box">
          <div class="progress-labels">
            <span>Execution Progress</span>
            <span style="font-family:'JetBrains Mono',monospace; font-weight:700; color:#0f172a;">${t.progress}%</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill ${fillClass}" style="width: ${t.progress}%;"></div>
          </div>
        </div>

        <!-- Meta Grid -->
        <div class="task-meta-grid">
          <div>
            <span class="meta-item-label">Assigned By</span>
            <span class="meta-item-val" style="font-family: inherit;">${escapeHtml(t.createdByName || 'Management')}</span>
          </div>
          <div>
            <span class="meta-item-label">Due Date</span>
            <span class="meta-item-val" style="${t.overdue ? 'color:#ef4444;' : ''}">
              ${t.formattedDueDate || 'No deadline'}
            </span>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="task-card-actions">
        ${canUpdateProgress ? `
          <button type="button" class="btn-card-action btn-update-progress" onclick="openUpdateProgressModal(${t.id})">
            <span>📈 Log Progress</span>
          </button>
        ` : ''}

        ${canSubmit ? `
          <button type="button" class="btn-card-action btn-submit-review" onclick="openSubmitReviewModal(${t.id})">
            <span>🚀 Submit Work</span>
          </button>
        ` : ''}

        ${isInReview ? `
          <div style="flex:1; text-align:center; font-size:12px; font-weight:700; color:#7c3aed; padding:6px 8px; background:#f5f3ff; border-radius:8px;">
            ⏳ Awaiting Manager Review
          </div>
        ` : ''}

        <button type="button" class="btn-card-action btn-view-history" onclick="openTaskHistoryModal(${t.id})" title="View Timeline & Updates">
          <span>📜 History</span>
        </button>
      </div>
    </div>
  `;
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

// Modal 1: Progress Update
function openUpdateProgressModal(taskId) {
  const task = allMyTasks.find(t => t.id === taskId);
  if (!task) return;

  document.getElementById('modalTaskId').value = task.id;
  document.getElementById('modalTaskTitle').textContent = task.title;
  document.getElementById('progressSlider').value = task.progress || 0;
  document.getElementById('sliderValDisplay').textContent = `${task.progress || 0}%`;
  document.getElementById('progressWorkDescription').value = '';

  openModal('updateProgressModal');
}

function syncSliderVal(val) {
  const clean = Math.min(100, Math.max(0, parseInt(val, 10) || 0));
  document.getElementById('sliderValDisplay').textContent = `${clean}%`;
}

function addProgress(inc) {
  const slider = document.getElementById('progressSlider');
  const current = parseInt(slider.value, 10) || 0;
  const next = Math.min(100, current + inc);
  slider.value = next;
  syncSliderVal(next);
}

function setProgressVal(val) {
  const slider = document.getElementById('progressSlider');
  slider.value = val;
  syncSliderVal(val);
}

async function submitProgressUpdate(e) {
  e.preventDefault();
  const taskId = document.getElementById('modalTaskId').value;
  const progressPercentage = parseInt(document.getElementById('progressSlider').value, 10);
  const workDescription = document.getElementById('progressWorkDescription').value.trim();

  if (!workDescription) {
    showToast('Please provide a description of the work completed.', 'error');
    return;
  }

  const btn = document.getElementById('btnSubmitProgress');
  btn.disabled = true;
  btn.textContent = 'Saving...';

  try {
    const res = await fetch(`/api/tasks/${taskId}/progress`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ progressPercentage, workDescription })
    });
    const json = await res.json();
    if (json.success) {
      showToast('Progress updated successfully!', 'success');
      closeModal('updateProgressModal');
      await loadMyTasks();
    } else {
      showToast(json.message || 'Failed to update progress', 'error');
    }
  } catch (err) {
    showToast('Network error while saving progress', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Save Progress Log';
  }
}

// Modal 2: Submit for Review
function openSubmitReviewModal(taskId) {
  const task = allMyTasks.find(t => t.id === taskId);
  if (!task) return;

  document.getElementById('submitModalTaskId').value = task.id;
  document.getElementById('submitModalTaskTitle').textContent = task.title;
  document.getElementById('submitWorkNotes').value = '';

  openModal('submitReviewModal');
}

async function executeSubmitForReview(e) {
  e.preventDefault();
  const taskId = document.getElementById('submitModalTaskId').value;
  const notes = document.getElementById('submitWorkNotes').value.trim();

  const btn = document.getElementById('btnConfirmSubmit');
  btn.disabled = true;
  btn.textContent = 'Submitting...';

  try {
    const res = await fetch(`/api/tasks/${taskId}/submit`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ notes })
    });
    const json = await res.json();
    if (json.success) {
      showToast('Work submitted for manager review!', 'success');
      closeModal('submitReviewModal');
      await loadMyTasks();
    } else {
      showToast(json.message || 'Submission failed', 'error');
    }
  } catch (err) {
    showToast('Network error during submission', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Submit for Review';
  }
}

// Modal 3: History & Audit Timeline
async function openTaskHistoryModal(taskId) {
  const task = allMyTasks.find(t => t.id === taskId);
  const overviewEl = document.getElementById('historyTaskOverview');
  const container = document.getElementById('historyTimelineContainer');

  if (overviewEl && task) {
    overviewEl.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:8px;">
        <h4 style="margin:0; font-size:17px; font-weight:800; color:#0f172a;">${escapeHtml(task.title)}</h4>
        <span class="task-badge badge-status-${(task.status || 'to_do').toLowerCase().replace('_', '-')}">${formatStatus(task.status)}</span>
      </div>
      <div style="font-size:13px; color:#64748b; line-height:1.5;">
        ${escapeHtml(task.description || 'No description.')}
      </div>
    `;
  }

  if (container) {
    container.innerHTML = '<div style="color:#64748b; font-size:13px;">Loading update history...</div>';
  }
  openModal('taskHistoryModal');

  try {
    const res = await fetch(`/api/tasks/${taskId}/history`, { credentials: 'include' });
    const json = await res.json();

    if (json.success && Array.isArray(json.data) && json.data.length > 0) {
      container.innerHTML = json.data.map(item => `
        <div class="timeline-item">
          <div class="timeline-dot"></div>
          <div class="timeline-content">
            <div class="timeline-header">
              <span class="timeline-author">${escapeHtml(item.updaterName || item.updaterEmail)}</span>
              <span class="timeline-time">${item.formattedTimestamp || ''}</span>
            </div>
            <div style="display:flex; gap:8px; align-items:center; margin-bottom:6px;">
              <span style="font-size:11.5px; font-weight:700; color:#0284c7; background:#e0f2fe; padding:2px 6px; border-radius:4px;">
                Progress: ${item.progressPercentage}%
              </span>
              ${item.newStatus ? `
                <span style="font-size:11.5px; font-weight:700; color:#475569; background:#f1f5f9; padding:2px 6px; border-radius:4px;">
                  Status: ${formatStatus(item.newStatus)}
                </span>
              ` : ''}
            </div>
            <div class="timeline-body">
              ${escapeHtml(item.workDescription || 'No description.')}
            </div>
          </div>
        </div>
      `).join('');
    } else {
      container.innerHTML = '<div style="color:#64748b; font-size:13px; font-style:italic;">No progress updates logged yet.</div>';
    }
  } catch (err) {
    container.innerHTML = '<div style="color:#ef4444; font-size:13px;">Error fetching task history.</div>';
  }
}

// Quick status filter helper
function setQuickStatusFilter(status) {
  currentFilterStatus = status;

  // Highlight card
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

  // Sync select element
  const select = document.getElementById('taskStatusSelect');
  if (select) select.value = status;

  loadMyTasks();
}

function handleFilterChange() {
  const statusSel = document.getElementById('taskStatusSelect');
  const prioritySel = document.getElementById('taskPrioritySelect');

  if (statusSel) currentFilterStatus = statusSel.value;
  if (prioritySel) currentFilterPriority = prioritySel.value;

  loadMyTasks();
}

function debounceSearch() {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    const searchInput = document.getElementById('taskSearchInput');
    if (searchInput) currentSearchQuery = searchInput.value;
    loadMyTasks();
  }, 300);
}

function resetFilters() {
  currentFilterStatus = 'ALL';
  currentFilterPriority = 'ALL';
  currentSearchQuery = '';

  const searchInput = document.getElementById('taskSearchInput');
  const statusSel = document.getElementById('taskStatusSelect');
  const prioritySel = document.getElementById('taskPrioritySelect');

  if (searchInput) searchInput.value = '';
  if (statusSel) statusSel.value = 'ALL';
  if (prioritySel) prioritySel.value = 'ALL';

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

// Toast helper
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
