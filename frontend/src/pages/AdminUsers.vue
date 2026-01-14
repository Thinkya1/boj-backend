<template>
  <div class="stack">
    <div class="card">
      <h2 class="section-title">用户管理</h2>
      <p class="muted">创建、更新与删除用户。</p>
    </div>

    <div class="card stack">
      <h3>{{ mode === 'edit' ? '编辑用户' : '新增用户' }}</h3>
      <div class="grid grid-2">
        <label class="stack" style="gap: 6px;">
          <span>昵称</span>
          <input v-model="form.userName" class="input" placeholder="用户昵称" />
        </label>
        <label v-if="mode === 'create'" class="stack" style="gap: 6px;">
          <span>账号</span>
          <input v-model="form.userAccount" class="input" placeholder="账号" />
        </label>
        <label class="stack" style="gap: 6px;">
          <span>角色</span>
          <select v-model="form.userRole" class="select">
            <option value="user">用户</option>
            <option value="admin">管理员</option>
            <option value="ban">封禁</option>
          </select>
        </label>
        <label class="stack" style="gap: 6px;">
          <span>头像 URL</span>
          <input v-model="form.userAvatar" class="input" placeholder="头像链接" />
        </label>
        <label class="stack" style="gap: 6px;">
          <span>简介</span>
          <textarea v-model="form.userProfile" class="textarea"></textarea>
        </label>
      </div>
      <div style="display: flex; gap: 12px;">
        <button class="btn btn-primary" :disabled="saving" @click="save">
          {{ saving ? '保存中...' : '保存' }}
        </button>
        <button class="btn btn-ghost" type="button" @click="resetForm">重置</button>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3>用户列表</h3>
        <button class="btn btn-ghost" @click="fetchList">刷新</button>
      </div>
      <div v-if="loading" class="muted">加载中...</div>
      <div v-else-if="!items.length" class="muted">暂无用户</div>
      <div v-else class="stack">
        <div v-for="item in items" :key="item.id" class="card" style="box-shadow: none;">
          <div class="card-header">
            <div>
              <div>{{ item.userName || item.userAccount || `用户${item.id}` }}</div>
              <div class="muted">ID：{{ item.id }} · 角色：{{ item.userRole }}</div>
            </div>
            <div style="display: flex; gap: 8px;">
              <button class="btn btn-ghost" @click="edit(item)">编辑</button>
              <button class="btn btn-danger" @click="remove(item)">删除</button>
            </div>
          </div>
          <div class="muted">简介：{{ item.userProfile || '-' }}</div>
        </div>
      </div>

      <Pagination :page="page" :page-size="pageSize" :total="total" @update:page="changePage" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { post } from '../api/http';
import { useToastStore } from '../stores/toast';
import Pagination from '../components/Pagination.vue';

const toast = useToastStore();
const items = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const saving = ref(false);
const mode = ref('create');

const form = reactive({
  id: null,
  userName: '',
  userAccount: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
});

async function fetchList() {
  loading.value = true;
  try {
    const data = await post('/user/list/page', {
      current: page.value,
      pageSize: pageSize.value,
      sortField: 'createTime',
      sortOrder: 'descend',
    });
    items.value = data.records || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

function changePage(next) {
  page.value = next;
  fetchList();
}

function resetForm() {
  mode.value = 'create';
  form.id = null;
  form.userName = '';
  form.userAccount = '';
  form.userAvatar = '';
  form.userProfile = '';
  form.userRole = 'user';
}

function edit(item) {
  mode.value = 'edit';
  form.id = item.id;
  form.userName = item.userName || '';
  form.userAvatar = item.userAvatar || '';
  form.userProfile = item.userProfile || '';
  form.userRole = item.userRole || 'user';
}

async function save() {
  saving.value = true;
  try {
    if (mode.value === 'create') {
      if (!form.userAccount) {
        toast.push('请输入账号', 'error');
        return;
      }
      await post('/user/add', {
        userName: form.userName,
        userAccount: form.userAccount,
        userAvatar: form.userAvatar,
        userRole: form.userRole,
      });
    } else {
      await post('/user/update', {
        id: form.id,
        userName: form.userName,
        userAvatar: form.userAvatar,
        userProfile: form.userProfile,
        userRole: form.userRole,
      });
    }
    toast.push('保存成功', 'info');
    resetForm();
    fetchList();
  } finally {
    saving.value = false;
  }
}

async function remove(item) {
  if (!confirm('确认删除该用户吗？')) {
    return;
  }
  await post('/user/delete', { id: item.id });
  toast.push('删除成功', 'info');
  fetchList();
}

onMounted(fetchList);
</script>
