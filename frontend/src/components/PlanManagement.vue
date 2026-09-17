<template>
  <div class="plan-management">
    <el-card class="card">
      <div class="card-header">
        <h2>团建方案管理</h2>
        <el-button type="primary" @click="openAddModal">
          <el-icon><Plus /></el-icon>
          添加方案
        </el-button>
      </div>

      <el-table :data="plans" border stripe class="plan-table">
        <el-table-column prop="planName" label="方案名称" width="180" />
        <el-table-column prop="transportation" label="交通方式" width="120" />
        <el-table-column prop="venue" label="场地" width="150" />
        <el-table-column prop="projects" label="项目内容" width="200" show-overflow-tooltip />
        <el-table-column prop="costPerPerson" label="人均费用" width="100">
          <template #default="{ row }">
            ¥{{ row.costPerPerson }}
          </template>
        </el-table-column>
        <el-table-column prop="durationDays" label="时长(天)" width="80" />
        <el-table-column prop="minParticipants" label="最小人数" width="80" />
        <el-table-column prop="maxParticipants" label="最大人数" width="80" />
        <el-table-column prop="suitableActivities" label="适配活动" width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editPlan(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deletePlan(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="isEdit ? '编辑方案' : '添加方案'" v-model="showAddModal" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="方案名称" prop="planName">
          <el-input v-model="form.planName" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="交通方式" prop="transportation">
          <el-input v-model="form.transportation" />
        </el-form-item>
        <el-form-item label="场地" prop="venue">
          <el-input v-model="form.venue" />
        </el-form-item>
        <el-form-item label="项目内容" prop="projects">
          <el-input v-model="form.projects" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="人均费用" prop="costPerPerson">
          <el-input-number v-model="form.costPerPerson" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时长天数" prop="durationDays">
          <el-input-number v-model="form.durationDays" :min="1" :max="30" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最小人数" prop="minParticipants">
          <el-input-number v-model="form.minParticipants" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最大人数" prop="maxParticipants">
          <el-input-number v-model="form.maxParticipants" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="适配活动" prop="suitableActivities">
          <el-input v-model="form.suitableActivities" type="textarea" :rows="2" />
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
import { planApi } from '../api';
import { ElMessage } from 'element-plus';
const emit = defineEmits(['refresh']);
const plans = ref([]);
const showAddModal = ref(false);
const isEdit = ref(false);
const formRef = ref();
const form = reactive({
 id: null,
 planName: '',
 description: '',
 transportation: '',
 venue: '',
 projects: '',
 costPerPerson: 0,
 durationDays: 1,
 minParticipants: 10,
 maxParticipants: 50,
 suitableActivities: ''
});
const rules = {
 planName: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
 transportation: [{ required: true, message: '请输入交通方式', trigger: 'blur' }],
 venue: [{ required: true, message: '请输入场地', trigger: 'blur' }],
 projects: [{ required: true, message: '请输入项目内容', trigger: 'blur' }],
 costPerPerson: [{ required: true, message: '请输入人均费用', trigger: 'blur' }],
 durationDays: [{ required: true, message: '请输入时长天数', trigger: 'blur' }],
 minParticipants: [{ required: true, message: '请输入最小人数', trigger: 'blur' }],
 maxParticipants: [{ required: true, message: '请输入最大人数', trigger: 'blur' }],
 suitableActivities: [{ required: true, message: '请输入适配活动', trigger: 'blur' }]
};
const loadPlans = async () => {
 try {
 const res = await planApi.getAllPlans();
 plans.value = res.data;
 }
 catch (error) {
 ElMessage.error('加载方案失败');
 }
};
const openAddModal = () => {
  isEdit.value = false;
  form.id = null;
  form.planName = '';
  form.description = '';
  form.transportation = '';
  form.venue = '';
  form.projects = '';
  form.costPerPerson = 0;
  form.durationDays = 1;
  form.minParticipants = 10;
  form.maxParticipants = 50;
  form.suitableActivities = '';
  showAddModal.value = true;
};
const editPlan = (row) => {
 isEdit.value = true;
 form.id = row.id;
 form.planName = row.planName;
 form.description = row.description || '';
 form.transportation = row.transportation;
 form.venue = row.venue;
 form.projects = row.projects;
 form.costPerPerson = row.costPerPerson;
 form.durationDays = row.durationDays;
 form.minParticipants = row.minParticipants;
 form.maxParticipants = row.maxParticipants;
 form.suitableActivities = row.suitableActivities;
 showAddModal.value = true;
};
const deletePlan = async (id) => {
 await ElMessage.confirm('确定要删除该方案吗?', '提示', {
 confirmButtonText: '确定',
 cancelButtonText: '取消',
 type: 'warning'
 });
 try {
 await planApi.deletePlan(id);
 ElMessage.success('删除成功');
 loadPlans();
 emit('refresh');
 }
 catch (error) {
 ElMessage.error('删除失败');
 }
};
const submitForm = async () => {
 if (!formRef.value)
 return;
 await formRef.value.validate(async (valid) => {
 if (valid) {
 try {
 if (isEdit.value) {
 await planApi.updatePlan(form.id, form);
 ElMessage.success('更新成功');
 }
 else {
 await planApi.createPlan(form);
 ElMessage.success('添加成功');
 }
 showAddModal.value = false;
 loadPlans();
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
 form.planName = '';
 form.description = '';
 form.transportation = '';
 form.venue = '';
 form.projects = '';
 form.costPerPerson = 0;
 form.durationDays = 1;
 form.minParticipants = 10;
 form.maxParticipants = 50;
 form.suitableActivities = '';
};
onMounted(() => {
 loadPlans();
});
</script>

<style scoped>
.plan-management {
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

.plan-table {
  width: 100%;
}
</style>