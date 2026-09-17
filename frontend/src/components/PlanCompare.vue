<template>
  <div class="plan-compare">
    <el-card class="constraint-card">
      <div class="card-header">
        <h2>约束条件设置</h2>
        <div class="header-actions">
          <el-button size="small" @click="loadFromCache">从缓存加载</el-button>
          <el-button size="small" type="warning" @click="saveToCache">保存到缓存</el-button>
          <el-tooltip content="开启后，修改约束条件自动重新对比" placement="top">
            <el-switch v-model="autoRefresh" active-text="实时刷新" inactive-text="手动对比" />
          </el-tooltip>
          <el-button v-if="!autoRefresh" size="small" type="primary" @click="comparePlans">开始对比</el-button>
        </div>
      </div>

      <el-form :model="constraintForm" :rules="rules" ref="formRef" label-width="120px" class="constraint-form">
        <div class="form-row">
          <el-form-item label="预算上限(模板)" prop="budgetLimit">
            <el-input-number v-model="constraintForm.budgetLimit" :min="0" :step="100" style="width: 200px" />
            <span class="form-hint">仅用于建模板/对账，不参与合规裁决</span>
          </el-form-item>
          <el-form-item label="最大出行天数" prop="maxDurationDays">
            <el-input-number v-model="constraintForm.maxDurationDays" :min="1" :max="30" style="width: 200px" />
          </el-form-item>
          <el-form-item label="参与人数" prop="participantCount">
            <el-input-number v-model="constraintForm.participantCount" :min="1" style="width: 200px" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="必备活动" prop="requiredActivities">
            <el-input v-model="constraintForm.requiredActivities" placeholder="多个活动用逗号分隔，如：户外拓展,聚餐" style="width: 450px" />
          </el-form-item>
        </div>
      </el-form>

      <div class="pool-bar" v-loading="poolLoading">
        <span class="pool-bar-item">池子总额：<b>¥{{ formatMoney(budgetPool.totalAmount) }}</b></span>
        <span class="pool-bar-item">已被批次占住：<b class="occupied">¥{{ formatMoney(budgetPool.occupiedAmount) }}</b></span>
        <span class="pool-bar-item">可占用余额（超预算只认它）：<b class="available">¥{{ formatMoney(budgetPool.availableAmount) }}</b></span>
        <el-button link type="primary" size="small" @click="loadPool">刷新余额</el-button>
      </div>
    </el-card>

    <el-card class="result-card" v-if="results.length > 0">
      <div class="card-header">
        <h2>方案对比结果</h2>
        <div class="filter-tabs">
          <el-button :type="showAll ? 'primary' : ''" @click="showAll = true">全部方案</el-button>
          <el-button :type="!showAll ? 'primary' : ''" @click="showAll = false">仅合规方案</el-button>
        </div>
      </div>

      <div class="result-stats">
        <el-statistic title="总方案数" :value="allResults.length" />
        <el-statistic title="合规方案数" :value="compliantCount" />
        <el-statistic title="最优适配度" :value="bestScore" suffix="%" />
      </div>

      <el-table :data="displayResults" border stripe class="compare-table" @row-click="handleRowClick"
                :row-class-name="rowClassName" highlight-current-row>
        <el-table-column prop="planName" label="方案名称" width="160">
          <template #default="{ row }">
            <span :class="{ 'best-plan': row.adaptabilityScore === maxScore }">{{ row.planName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="transportation" label="交通方式" width="120" />
        <el-table-column prop="venue" label="场地" width="140" />
        <el-table-column prop="projects" label="项目内容" width="200" show-overflow-tooltip />
        <el-table-column prop="costPerPerson" label="人均费用" width="100">
          <template #default="{ row }">¥{{ row.costPerPerson }}</template>
        </el-table-column>
        <el-table-column prop="totalCost" label="总费用" width="100">
          <template #default="{ row }">¥{{ row.totalCost }}</template>
        </el-table-column>
        <el-table-column prop="durationDays" label="时长(天)" width="80" />
        <el-table-column label="已落地场次" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.activeBatchCount > 0" type="warning" size="small">
              {{ row.activeBatchCount }} 条生效
            </el-tag>
            <el-tooltip v-if="row.activeBatchCount > 0" :content="(row.activeBatchDates || []).join('、')" placement="top">
              <div class="batch-dates">{{ (row.activeBatchDates || []).slice(0, 2).join('、') }}{{ row.activeBatchDates.length > 2 ? ' …' : '' }}</div>
            </el-tooltip>
            <span v-else class="muted">无</span>
          </template>
        </el-table-column>
        <el-table-column label="约束校验" width="280">
          <template #default="{ row }">
            <div class="constraint-checks">
              <el-tag :type="row.isBudgetCompliant ? 'success' : 'danger'" size="small">预算</el-tag>
              <el-tag :type="row.isDurationCompliant ? 'success' : 'danger'" size="small">天数</el-tag>
              <el-tag :type="row.isParticipantCountCompliant ? 'success' : 'danger'" size="small">人数</el-tag>
              <el-tag :type="row.isRequiredActivitiesCompliant ? 'success' : 'danger'" size="small">活动</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="合规状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isAllCompliant ? 'success' : 'danger'" size="small">
              {{ row.isAllCompliant ? '合规' : '不合规' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="适配度" width="120">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(row.adaptabilityScore * 100)" :color="getProgressColor(row.adaptabilityScore)" />
          </template>
        </el-table-column>
        <el-table-column label="适配度分数" width="100">
          <template #default="{ row }">
            <span :class="{ 'high-score': row.adaptabilityScore >= 0.7 }">{{ (row.adaptabilityScore * 100).toFixed(1) }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="落地操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success"
                       @click.stop="openLandDialog(row)">落地成团</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="compliance-detail" v-if="selectedPlan">
        <div class="detail-header">
          <h3>{{ selectedPlan.planName }} - 约束校验详情</h3>
          <el-button size="small" @click="selectedPlan = null">关闭详情</el-button>
        </div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">预算校验（落地口径·池子余额）:</span>
            <span :class="selectedPlan.isBudgetCompliant ? 'success' : 'danger'">
              {{ selectedPlan.isBudgetCompliant ? '满足' : '不满足' }}
              (池子可占用余额 ¥{{ formatMoney(budgetPool.availableAmount) }}，按当次人数预估总费用 ¥{{ selectedPlan.totalCost }})
            </span>
          </div>
          <div class="detail-item">
            <span class="label">预算校验（对账口径·模板上限）:</span>
            <span :class="selectedPlan.isTemplateBudgetCompliant ? 'success' : 'danger'">
              {{ selectedPlan.isTemplateBudgetCompliant ? '满足' : '不满足' }}
              (模板上限 ¥{{ constraintForm.budgetLimit }}，仅供与建模板的数字对账，不决定合规)
            </span>
          </div>
          <div class="detail-item">
            <span class="label">天数校验:</span>
            <span :class="selectedPlan.isDurationCompliant ? 'success' : 'danger'">
              {{ selectedPlan.isDurationCompliant ? '满足' : '不满足' }}
              (限制 {{ constraintForm.maxDurationDays }} 天，方案 {{ selectedPlan.durationDays }} 天)
            </span>
          </div>
          <div class="detail-item">
            <span class="label">人数校验:</span>
            <span :class="selectedPlan.isParticipantCountCompliant ? 'success' : 'danger'">
              {{ selectedPlan.isParticipantCountCompliant ? '满足' : '不满足' }}
              (需求 {{ constraintForm.participantCount }} 人，方案支持 {{ selectedPlan.minParticipants }}-{{ selectedPlan.maxParticipants }} 人)
            </span>
          </div>
          <div class="detail-item">
            <span class="label">活动校验:</span>
            <span :class="selectedPlan.isRequiredActivitiesCompliant ? 'success' : 'danger'">
              {{ selectedPlan.isRequiredActivitiesCompliant ? '满足' : '不满足' }}
              (必备: {{ constraintForm.requiredActivities || '无' }}，适配: {{ selectedPlan.suitableActivities }})
            </span>
          </div>
          <div class="detail-item" v-if="selectedPlan.compliantItems && selectedPlan.compliantItems.length > 0">
            <span class="label">合规项:</span>
            <div class="tag-list">
              <el-tag v-for="item in selectedPlan.compliantItems" :key="item" type="success" size="small" class="tag-item">{{ item }}</el-tag>
            </div>
          </div>
          <div class="detail-item" v-if="selectedPlan.nonCompliantItems && selectedPlan.nonCompliantItems.length > 0">
            <span class="label">不合规项:</span>
            <div class="tag-list">
              <el-tag v-for="item in selectedPlan.nonCompliantItems" :key="item" type="danger" size="small" class="tag-item">{{ item }}</el-tag>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-empty v-else description="请设置约束条件并点击开始对比，或开启实时刷新自动对比" />

    <el-dialog title="落地成团" v-model="showLandDialog" width="520px">
      <div v-if="landRow" class="land-dialog">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="方案">{{ landRow.planName }}</el-descriptions-item>
          <el-descriptions-item label="场地">{{ landRow.venue }}</el-descriptions-item>
          <el-descriptions-item label="人均费用">¥{{ landRow.costPerPerson }}</el-descriptions-item>
          <el-descriptions-item label="支持人数">{{ landRow.minParticipants }} - {{ landRow.maxParticipants }} 人</el-descriptions-item>
          <el-descriptions-item label="池子可占用余额">
            <span class="available">¥{{ formatMoney(budgetPool.availableAmount) }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-form :model="landForm" label-width="110px" class="land-form">
          <el-form-item label="出行日期" required>
            <el-date-picker v-model="landForm.travelDate" type="date" value-format="YYYY-MM-DD"
                            placeholder="选择出行日期" style="width: 100%" />
          </el-form-item>
          <el-form-item label="成团人数" required>
            <el-input-number v-model="landForm.groupSize" :min="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="本次将扣">
            <span class="land-amount">¥{{ formatMoney(landAmount) }}</span>
            <span class="form-hint"> = ¥{{ landRow.costPerPerson }} × {{ landForm.groupSize || 0 }} 人</span>
          </el-form-item>
          <el-form-item label="落地后余额">
            <span :class="landAmount > Number(budgetPool.availableAmount) ? 'danger-text' : 'success-text'">
              ¥{{ formatMoney(Number(budgetPool.availableAmount || 0) - landAmount) }}
            </span>
            <span v-if="landAmount > Number(budgetPool.availableAmount)" class="danger-text">
              （超出可占用余额，落不下去）
            </span>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showLandDialog = false">取消</el-button>
        <el-button type="success" :loading="landing" @click="submitLand">确认落地并扣款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { planApi, constraintApi, budgetApi, batchApi } from '../api'
import { ElMessage } from 'element-plus'

const formRef = ref()
const results = ref([])
const allResults = ref([])
const showAll = ref(true)
const selectedPlan = ref(null)
const autoRefresh = ref(false)
const budgetPool = reactive({ totalAmount: 0, occupiedAmount: 0, availableAmount: 0 })
const poolLoading = ref(false)
const constraintForm = reactive({
  templateName: '临时约束',
  budgetLimit: 50000,
  maxDurationDays: 3,
  participantCount: 30,
  requiredActivities: ''
})

// 落地成团对话框状态：方案只能通过真正落成批次来"选定"，
// 不存在只勾一个已选定、背后没有批次也不进出钱的改法
const showLandDialog = ref(false)
const landing = ref(false)
const landRow = ref(null)
const landForm = reactive({ travelDate: '', groupSize: 30 })

const formatMoney = (v) => {
  if (v === null || v === undefined) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const landAmount = computed(() => {
  if (!landRow.value) return 0
  return Number(landRow.value.costPerPerson) * (landForm.groupSize || 0)
})

const rules = {
  budgetLimit: [{ required: true, message: '请输入预算上限', trigger: 'blur' }],
  maxDurationDays: [{ required: true, message: '请输入最大出行天数', trigger: 'blur' }],
  participantCount: [{ required: true, message: '请输入参与人数', trigger: 'blur' }]
}

const compliantCount = computed(() => {
  return allResults.value.filter(r => r.isAllCompliant).length
})

const maxScore = computed(() => {
  if (allResults.value.length === 0) return 0
  return Math.max(...allResults.value.map(r => r.adaptabilityScore))
})

const bestScore = computed(() => {
  if (allResults.value.length === 0) return 0
  return (maxScore.value * 100).toFixed(1)
})

const displayResults = computed(() => {
  if (showAll.value) {
    return results.value
  }
  return results.value.filter(r => r.isAllCompliant)
})

const loadPool = async () => {
  poolLoading.value = true
  try {
    const res = await budgetApi.getPool()
    Object.assign(budgetPool, res.data)
  } catch (e) {
    // 池子加载失败不阻断页面，落地时服务端还会再校验
  } finally {
    poolLoading.value = false
  }
}

const comparePlans = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 每次对比都拿最新池子余额，已扣出去的钱不会再把第二套方案筛成合规
        await loadPool()
        const res = await planApi.compareAndFilter(constraintForm)
        allResults.value = res.data.allResults
        results.value = res.data.allResults
        if (res.data.budgetPool) {
          Object.assign(budgetPool, res.data.budgetPool)
        }
        if (selectedPlan.value) {
          const updated = results.value.find(r => r.planId === selectedPlan.value.planId)
          if (updated) {
            selectedPlan.value = updated
          } else {
            selectedPlan.value = null
          }
        }
        ElMessage.success(`对比完成，共 ${res.data.totalCount} 个方案，其中 ${res.data.compliantCount} 个合规（预算按池子可占用余额 ¥${formatMoney(budgetPool.availableAmount)} 判定）`)
      } catch (error) {
        ElMessage.error('对比失败')
      }
    }
  })
}

const openLandDialog = (row) => {
  landRow.value = row
  landForm.travelDate = ''
  landForm.groupSize = constraintForm.participantCount || row.minParticipants
  showLandDialog.value = true
}

const submitLand = async () => {
  if (!landForm.travelDate) {
    ElMessage.warning('请选择出行日期')
    return
  }
  if (!landForm.groupSize || landForm.groupSize <= 0) {
    ElMessage.warning('请输入成团人数')
    return
  }
  landing.value = true
  try {
    const payload = {
      planId: landRow.value.planId,
      travelDate: landForm.travelDate,
      groupSize: landForm.groupSize,
      maxDurationDays: constraintForm.maxDurationDays,
      requiredActivities: constraintForm.requiredActivities || ''
    }
    const res = await batchApi.land(payload)
    ElMessage.success(`落地成功：批次 ${res.data.batchNo}，已从预算池扣下 ¥${formatMoney(res.data.lockedAmount)}`)
    showLandDialog.value = false
    landRow.value = null
    // 关掉页面再进来三处要对得上：立即刷新池子和对比结果
    await comparePlans()
  } catch (error) {
    const msg = error.response?.data?.message || '落地失败'
    ElMessage.error(msg)
    // 场地被并发抢占或余额变化时刷新，保证后到的人看到的是最新状态
    await loadPool()
  } finally {
    landing.value = false
  }
}

const handleRowClick = (row) => {
  selectedPlan.value = row
}

const rowClassName = ({ row }) => {
  if (selectedPlan.value && row.planId === selectedPlan.value.planId) {
    return 'selected-row'
  }
  return ''
}

const loadFromCache = async () => {
  try {
    const res = await constraintApi.getFromRedis('current')
    if (res && res.data) {
      constraintForm.budgetLimit = res.data.budgetLimit
      constraintForm.maxDurationDays = res.data.maxDurationDays
      constraintForm.participantCount = res.data.participantCount
      constraintForm.requiredActivities = res.data.requiredActivities || ''
      ElMessage.success('已从缓存加载约束条件')
    } else {
      ElMessage.warning('缓存中没有可用的约束条件')
    }
  } catch (error) {
    ElMessage.error('加载缓存失败')
  }
}

const saveToCache = async () => {
  try {
    await constraintApi.saveToRedis('current', constraintForm)
    ElMessage.success('约束条件已保存到缓存')
  } catch (error) {
    ElMessage.error('保存缓存失败')
  }
}

const getProgressColor = (score) => {
  if (score >= 0.7) return '#67c23a'
  if (score >= 0.4) return '#e6a23c'
  return '#f56c6c'
}

watch(constraintForm, () => {
  if (autoRefresh.value && results.value.length > 0) {
    comparePlans()
  }
}, { deep: true })

watch(autoRefresh, (newVal) => {
  if (newVal && results.value.length === 0) {
    comparePlans()
  }
})

onMounted(() => {
  comparePlans()
})
</script>

<style scoped>
.plan-compare {
  padding: 0;
}

.constraint-card {
  margin-bottom: 20px;
}

.result-card {
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

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.constraint-form {
  margin-bottom: 0;
}

.form-hint {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.pool-bar {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 16px;
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 8px;
  font-size: 14px;
}

.pool-bar-item b {
  font-size: 15px;
}

.pool-bar-item .occupied {
  color: #f56c6c;
}

.pool-bar-item .available {
  color: #67c23a;
}

.batch-dates {
  font-size: 11px;
  color: #e6a23c;
  margin-top: 2px;
}

.muted {
  color: #c0c4cc;
  font-size: 12px;
}

.land-dialog .land-form {
  margin-top: 16px;
}

.land-amount {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}

.danger-text {
  color: #f56c6c;
  font-weight: 600;
}

.success-text {
  color: #67c23a;
  font-weight: 600;
}

.available {
  color: #67c23a;
  font-weight: 600;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.filter-tabs {
  display: flex;
  gap: 10px;
}

.result-stats {
  display: flex;
  gap: 40px;
  margin-bottom: 20px;
  padding: 15px;
  background: #fafafa;
  border-radius: 8px;
}

.compare-table {
  width: 100%;
  cursor: pointer;
}

.best-plan {
  font-weight: bold;
  color: #667eea;
  font-size: 15px;
}

.constraint-checks {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.high-score {
  font-weight: bold;
  color: #67c23a;
  font-size: 15px;
}

.compliance-detail {
  margin-top: 20px;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.detail-header h3 {
  font-size: 16px;
  margin: 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.detail-item .label {
  font-weight: 500;
  color: #666;
}

.detail-item .success {
  color: #67c23a;
  font-weight: 500;
}

.detail-item .danger {
  color: #f56c6c;
  font-weight: 500;
}

.tag-list {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

:deep(.selected-row) {
  background-color: #ecf5ff !important;
}
</style>