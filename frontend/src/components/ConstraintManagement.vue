<template>
  <div class="constraint-management">
    <el-card class="card">
      <div class="card-header">
        <h2>约束条件管理</h2>
        <el-button type="primary" @click="openAddModal">
          <el-icon><Plus /></el-icon>
          添加约束模板
        </el-button>
      </div>

      <el-table :data="templates" border stripe class="template-table">
        <el-table-column prop="templateName" label="模板名称" width="180" />
        <el-table-column prop="budgetLimit" label="预算上限" width="120">
          <template #default="{ row }">
            ¥{{ row.budgetLimit }}
          </template>
        </el-table-column>
        <el-table-column prop="maxDurationDays" label="最大出行天数" width="120" />
        <el-table-column prop="participantCount" label="参与人数" width="100" />
        <el-table-column prop="requiredActivities" label="必备活动" width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editTemplate(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteTemplate(row.id)">删除</el-button>
            <el-button size="small" type="success" @click="useTemplate(row)">使用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="isEdit ? '编辑约束模板' : '添加约束模板'" v-model="showAddModal" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" />
        </el-form-item>
        <el-form-item label="预算上限" prop="budgetLimit">
          <el-input-number v-model="form.budgetLimit" :min="0" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最大出行天数" prop="maxDurationDays">
          <el-input-number v-model="form.maxDurationDays" :min="1" :max="30" style="width: 100%" />
        </el-form-item>
        <el-form-item label="参与人数" prop="participantCount">
          <el-input-number v-model="form.participantCount" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="必备活动" prop="requiredActivities">
          <el-input v-model="form.requiredActivities" type="textarea" :rows="2" placeholder="多个活动用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddModal = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>import { ref, reactive, onMounted } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { constraintApi } from '../api';
import { ElMessage } from 'element-plus';
const emit = defineEmits(['refresh']);
const templates = ref([]);
const showAddModal = ref(false);
const isEdit = ref(false);
const formRef = ref();
const form = reactive({
 id: null,
 templateName: '',
 budgetLimit: 0,
 maxDurationDays: 1,
 participantCount: 20,
 requiredActivities: ''
});
const rules = {
 templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
 budgetLimit: [{ required: true, message: '请输入预算上限', trigger: 'blur' }],
 maxDurationDays: [{ required: true, message: '请输入最大出行天数', trigger: 'blur' }],
 participantCount: [{ required: true, message: '请输入参与人数', trigger: 'blur' }]
};
const loadTemplates = async () => {
 try {
 const res = await constraintApi.getAllTemplates();
 templates.value = res.data;
 }
 catch (error) {
 ElMessage.error('加载模板失败');
 }
};
const openAddModal = () => {
  isEdit.value = false;
  form.id = null;
  form.templateName = '';
  form.budgetLimit = 0;
  form.maxDurationDays = 1;
  form.participantCount = 20;
  form.requiredActivities = '';
  showAddModal.value = true;
};
const editTemplate = (row) => {
 isEdit.value = true;
 form.id = row.id;
 form.templateName = row.templateName;
 form.budgetLimit = row.budgetLimit;
 form.maxDurationDays = row.maxDurationDays;
 form.participantCount = row.participantCount;
 form.requiredActivities = row.requiredActivities || '';
 showAddModal.value = true;
};
const deleteTemplate = async (id) => {
 await ElMessage.confirm('确定要删除该模板吗?', '提示', {
 confirmButtonText: '确定',
 cancelButtonText: '取消',
 type: 'warning'
 });
 try {
 await constraintApi.deleteTemplate(id);
 ElMessage.success('删除成功');
 loadTemplates();
 emit('refresh');
 }
 catch (error) {
 ElMessage.error('删除失败');
 }
};
const useTemplate = async (row) => {
 try {
 const constraintData = {
 templateName: row.templateName,
 budgetLimit: row.budgetLimit,
 maxDurationDays: row.maxDurationDays,
 participantCount: row.participantCount,
 requiredActivities: row.requiredActivities
 };
 await constraintApi.saveToRedis('current', constraintData);
 ElMessage.success('已保存到缓存，可在方案对比页面使用');
 }
 catch (error) {
 ElMessage.error('保存失败');
 }
};
const submitForm = async () => {
 if (!formRef.value)
 return;
 await formRef.value.validate(async (valid) => {
 if (valid) {
 try {
 if (isEdit.value) {
 await constraintApi.updateTemplate(form.id, form);
 ElMessage.success('更新成功');
 }
 else {
 await constraintApi.createTemplate(form);
 ElMessage.success('添加成功');
 }
 showAddModal.value = false;
 loadTemplates();
 emit('refresh');
 }
 catch (error) {
 ElMessage.error(isEdit.value ? '更新失败' : '添加失败');
 }
 }
 });
};
const resetForm = () => {
 form.id = null;
 form.templateName = '';
 form.budgetLimit = 0;
 form.maxDurationDays = 1;
 form.participantCount = 20;
 form.requiredActivities = '';
};
const formatDate = (dateStr) => {
 if (!dateStr)
 return '';
 const date = new Date(dateStr);
 return date.toLocaleString('zh-CN', {
 year: 'numeric',
 month: '2-digit',
 day: '2-digit',
 hour: '2-digit',
 minute: '2-digit'
 });
};
onMounted(() => {
 loadTemplates();
});
</script>

<style scoped>
.constraint-management {
  padding: 0;
}

.card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h2 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.template-table {
  width: 100%;
}
</style>