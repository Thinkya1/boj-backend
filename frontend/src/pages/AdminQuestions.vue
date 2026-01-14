<template>
  <div class="stack">
    <div class="card">
      <h2 class="section-title">题目管理</h2>
      <p class="muted">仅管理员可见，支持完整字段维护。</p>
    </div>

    <div class="card stack">
      <h3>{{ editing ? '编辑题目' : '选择题目以编辑' }}</h3>
      <div v-if="!editing" class="muted">在下方列表选择题目。</div>
      <form v-else class="stack" @submit.prevent="save">
        <label class="stack" style="gap: 6px;">
          <span>标题</span>
          <input v-model="form.title" class="input" />
        </label>
        <label class="stack" style="gap: 6px;">
          <span>标签</span>
          <TagInput v-model="form.tags" />
        </label>
        <label class="stack" style="gap: 6px;">
          <span>内容</span>
          <textarea v-model="form.content" class="textarea"></textarea>
        </label>
        <label class="stack" style="gap: 6px;">
          <span>标准答案</span>
          <textarea v-model="form.answer" class="textarea"></textarea>
        </label>
        <div class="grid grid-3">
          <label class="stack" style="gap: 6px;">
            <span>时间限制 (ms)</span>
            <input v-model.number="form.judgeConfig.timeLimit" type="number" class="input" />
          </label>
          <label class="stack" style="gap: 6px;">
            <span>内存限制 (KB)</span>
            <input v-model.number="form.judgeConfig.memoryLimit" type="number" class="input" />
          </label>
          <label class="stack" style="gap: 6px;">
            <span>栈限制 (KB)</span>
            <input v-model.number="form.judgeConfig.stackLimit" type="number" class="input" />
          </label>
        </div>
        <div class="stack">
          <h4>用例</h4>
          <div v-for="(item, index) in form.judgeCase" :key="index" class="card" style="box-shadow: none;">
            <div class="card-header">
              <div>用例 {{ index + 1 }}</div>
              <button type="button" class="btn btn-ghost" @click="removeCase(index)">删除</button>
            </div>
            <div class="grid grid-2">
              <label class="stack" style="gap: 6px;">
                <span>输入</span>
                <textarea v-model="item.input" class="textarea"></textarea>
              </label>
              <label class="stack" style="gap: 6px;">
                <span>输出</span>
                <textarea v-model="item.output" class="textarea"></textarea>
              </label>
            </div>
          </div>
          <button type="button" class="btn btn-ghost" @click="addCase">添加用例</button>
        </div>
        <div style="display: flex; gap: 12px;">
          <button class="btn btn-primary" :disabled="saving" type="submit">{{ saving ? '保存中...' : '保存' }}</button>
          <button class="btn btn-ghost" type="button" @click="cancel">取消</button>
        </div>
      </form>
    </div>

    <div class="card">
      <div class="card-header">
        <h3>题目列表</h3>
        <button class="btn btn-ghost" @click="fetchList">刷新</button>
      </div>
      <div v-if="loading" class="muted">加载中...</div>
      <div v-else-if="!items.length" class="muted">暂无题目</div>
      <div v-else class="stack">
        <div v-for="item in items" :key="item.id" class="card" style="box-shadow: none;">
          <div class="card-header">
            <div>
              <div>{{ item.title }}</div>
              <div class="muted">ID：{{ item.id }} · 创建者：{{ item.userId }}</div>
            </div>
            <div style="display: flex; gap: 8px;">
              <button class="btn btn-ghost" @click="select(item)">编辑</button>
              <button class="btn btn-danger" @click="remove(item)">删除</button>
            </div>
          </div>
          <div>
            <span v-for="tag in parseTags(item.tags)" :key="tag" class="tag">{{ tag }}</span>
          </div>
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
import TagInput from '../components/TagInput.vue';

const toast = useToastStore();
const items = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const saving = ref(false);
const editing = ref(false);

const form = reactive({
  id: null,
  title: '',
  content: '',
  tags: [],
  answer: '',
  judgeConfig: {
    timeLimit: 1000,
    memoryLimit: 262144,
    stackLimit: 262144,
  },
  judgeCase: [],
});

function safeParse(value, fallback) {
  if (!value) {
    return fallback;
  }
  try {
    return JSON.parse(value);
  } catch (error) {
    return fallback;
  }
}

function parseTags(value) {
  const list = safeParse(value, []);
  return Array.isArray(list) ? list : [];
}

async function fetchList() {
  loading.value = true;
  try {
    const data = await post('/question/list/page', {
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

function select(item) {
  editing.value = true;
  form.id = item.id;
  form.title = item.title || '';
  form.content = item.content || '';
  form.tags = parseTags(item.tags);
  form.answer = item.answer || '';
  form.judgeConfig = safeParse(item.judgeConfig, form.judgeConfig);
  form.judgeCase = safeParse(item.judgeCase, []);
}

function addCase() {
  form.judgeCase.push({ input: '', output: '' });
}

function removeCase(index) {
  form.judgeCase.splice(index, 1);
}

function cancel() {
  editing.value = false;
  form.id = null;
}

async function save() {
  saving.value = true;
  try {
    await post('/question/update', {
      id: form.id,
      title: form.title,
      content: form.content,
      tags: form.tags,
      answer: form.answer,
      judgeCase: form.judgeCase,
      judgeConfig: form.judgeConfig,
    });
    toast.push('保存成功', 'info');
    editing.value = false;
    fetchList();
  } finally {
    saving.value = false;
  }
}

async function remove(item) {
  if (!confirm('确认删除该题目吗？')) {
    return;
  }
  await post('/question/delete', { id: item.id });
  toast.push('删除成功', 'info');
  fetchList();
}

onMounted(fetchList);
</script>
