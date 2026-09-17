<template>
  <div class="batch-management">
    <el-card class="card pool-card">
      <div class="card-header">
        <h2>公司团建预算池</h2>
        <el-button type="primary" size="small" @click="openAdjustDialog">调整总额</el-button>
      </div>
      <div class="pool-stats" v-loading="poolLoading">
        <div class="pool-item">
          <div class="pool-label">池子总额</div>
          <div class="pool-value total">¥{{ formatMoney(pool.totalAmount) }}</div>
        </div>
        <div class="pool-item">
          <div class="pool-label">已被生效批次占住</div>
          <div class="pool-value occupied">¥{{ formatMoney(pool.occupiedAmount) }}</div>
        </div>
        <div class="pool-item">
          <div class="pool-label">可占用余额（落地认这个）</div>
          <div class="pool-value available">¥{{ formatMoney(pool.availableAmount) }}</div>
        </div>
      </div>
      <div class="pool-note">
        模板上的预算上限只用于建模板；对比是否超预算、批次能否落地，只认这里的可占用余额。
      </div>
    </el-card>

    <el-card class="card">
      <div class="card-header">
        <h2>落地批次台账</h2>
        <div class="header-actions">
          <el-radio-group v-model="statusFilter" size="small" @change="loadBatches">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="ACTIVE">生效中</el-radio-button>
            <el-radio-button label="INVALID">已失效</el-radio-button>
          </el-radio-group>
          <el-button size="small" @click="refreshAll">刷新</el-button>
        </div>
      </div>

      <el-table :data="batches" border stripe v-loading="batchLoading">
        <el-table-column prop="batchNo" label="批次号" width="180" />
        <el-table-column prop="planName" label="方案" width="160" show-overflow-tooltip />
        <el-table-column prop="venue" label="场地" width="140" show-overflow-tooltip />
        <el-table-column prop="travelDate" label="出行日期" width="120" />
        <el-table-column prop="groupSize" label="成团人数" width="90" />
        <el-table-column label="锁定人均" width="100">
          <template #default="{ row }">¥{{ row.lockedCostPerPerson }}</template>
        </el-table-column>
        <el-table-column label="扣下金额" width="120">
          <template #default="{ row }">
            <span class="hold-amount">¥{{ formatMoney(row.lockedAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '生效中' : '已失效' }}
            </el-tag>
            <div v-if="row.invalidReason === 'PLAN_COST_CHANGED'" class="invalid-reason">
              费用变动·已退款
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="落地时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="供应商对接" width="130" fixed="right">
          <template #default="{ row }">
            <el-tooltip v-if="row.status !== 'ACTIVE'" content="批次已失效、预算已退回，不能再对接场地供应商" placement="top">
              <el-button size="small" type="info" disabled>对接供应商</el-button>
            </el-tooltip>
            <el-button v-else size="small" type="success" @click="contactSupplier(row)">对接供应商</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="card">
      <div class="card-header">
        <h2>预算进出流水</h2>
        <el-button size="small" @click="loadTransactions">刷新</el-button>
      </div>
      <el-table :data="transactions" border stripe v-loading="txLoading">
        <el-table-column prop="id" label="流水号" width="100" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="txTagType(row.type)" size="small">{{ txTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="batchId" label="批次ID" width="100">
          <template #default="{ row }">{{ row.batchId || '—' }}</template>
        </el-table-column>
        <el-table-column prop="planId" label="方案ID" width="100">
          <template #default="{ row }">{{ row.planId || '—' }}</template>
        </el-table-column>
        <el-table-column label="金额（占用为正/退回为负）" width="200">
          <template #default="{ row }">
            <span :class="row.amount >= 0 ? 'amount-positive' : 'amount-negative'">
              {{ row.amount >= 0 ? '+' : '' }}¥{{ formatMoney(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog title="调整预算池总额" v-model="showAdjustDialog" width="420px">
      <el-form label-width="100px">
        <el-form-item label="新总额">
          <el-input-number v-model="adjustForm.totalAmount" :min="0" :step="10000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="adjustForm.remark" placeholder="如：Q3 追加团建预算" />
        </el-form-item>
        <div class="adjust-tip">不能把总额调到低于当前已被占住的 ¥{{ formatMoney(pool.occupiedAmount) }}</div>
      </el-form>
      <template #footer>
        <el-button @click="showAdjustDialog = false">取消</el-button>
        <el-button type="primary" :loading="adjusting" @click="submitAdjust">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { budgetApi, batchApi } from '../api'
import { ElMessage } from 'element-plus'

const pool = reactive({ totalAmount: 0, occupiedAmount: 0, availableAmount: 0 })
const batches = ref([])
const transactions = ref([])
const statusFilter = ref('')
const poolLoading = ref(false)
const batchLoading = ref(false)
const txLoading = ref(false)

const showAdjustDialog = ref(false)
const adjusting = ref(false)
const adjustForm = reactive({ totalAmount: 0, remark: '' })

const formatMoney = (v) => {
  if (v === null || v === undefined) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const formatDateTime = (v) => {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 19)
}

const loadPool = async () => {
  poolLoading.value = true
  try {
    const res = await budgetApi.getPool()
    Object.assign(pool, res.data)
  } catch (e) {
    ElMessage.error('预算池加载失败')
  } finally {
    poolLoading.value = false
  }
}

const loadBatches = async () => {
  batchLoading.value = true
  try {
    const res = await batchApi.listBatches(statusFilter.value)
    batches.value = res.data
  } catch (e) {
    ElMessage.error('批次台账加载失败')
  } finally {
    batchLoading.value = false
  }
}

const loadTransactions = async () => {
  txLoading.value = true
  try {
    const res = await budgetApi.getTransactions()
    transactions.value = res.data
  } catch (e) {
    ElMessage.error('预算流水加载失败')
  } finally {
    txLoading.value = false
  }
}

const refreshAll = () => {
  loadPool()
  loadBatches()
  loadTransactions()
}

const openAdjustDialog = () => {
  adjustForm.totalAmount = Number(pool.totalAmount)
  adjustForm.remark = ''
  showAdjustDialog.value = true
}

const submitAdjust = async () => {
  adjusting.value = true
  try {
    await budgetApi.adjustTotal({
      totalAmount: adjustForm.totalAmount,
      remark: adjustForm.remark || '行政调整预算池总额'
    })
    ElMessage.success('预算池总额已调整')
    showAdjustDialog.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '调整失败')
  } finally {
    adjusting.value = false
  }
}

const contactSupplier = async (row) => {
  try {
    const res = await batchApi.contactSupplier(row.id)
    ElMessage.success(`批次 ${res.data.batchNo} 生效中，已放行对接场地【${res.data.venue}】（${res.data.travelDate}）`)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '该批次不能对接供应商')
    loadBatches()
  }
}

const txTypeText = (t) => ({ HOLD: '落地占用', REFUND: '失效退回', ADJUST: '总额调整' }[t] || t)
const txTagType = (t) => ({ HOLD: 'danger', REFUND: 'success', ADJUST: 'warning' }[t] || '')

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
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

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pool-stats {
  display: flex;
  gap: 40px;
  padding: 10px 0;
}

.pool-item {
  flex: 1;
  text-align: center;
  padding: 15px;
  background: #f7fafc;
  border-radius: 8px;
}

.pool-label {
  color: #666;
  font-size: 13px;
  margin-bottom: 8px;
}

.pool-value {
  font-size: 24px;
  font-weight: 700;
}

.pool-value.total { color: #409eff; }
.pool-value.occupied { color: #f56c6c; }
.pool-value.available { color: #67c23a; }

.pool-note {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.hold-amount {
  color: #f56c6c;
  font-weight: 600;
}

.invalid-reason {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.amount-positive {
  color: #f56c6c;
  font-weight: 600;
}

.amount-negative {
  color: #67c23a;
  font-weight: 600;
}

.adjust-tip {
  font-size: 12px;
  color: #e6a23c;
}
</style>
