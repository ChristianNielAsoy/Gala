package com.example.gala.ui.expenseeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gala.data.database.entities.Expense
import com.example.gala.data.database.entities.ExpenseItem
import com.example.gala.data.database.entities.ExpenseItemShare
import com.example.gala.data.database.entities.ExpenseShare
import com.example.gala.util.DateUtils
import java.util.UUID

@Composable
fun ExpenseEditorUI(
    tripId: String,
    templateTitle: String? = null,
    onSaveExpense: (
        expense: Expense,
        items: List<ExpenseItem>,
        perItemShares: List<ExpenseItemShare>,
        finalShares: List<ExpenseShare>
    ) -> Unit
) {
    var description by rememberSaveable(templateTitle) { mutableStateOf(templateTitle ?: "") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf("") }

    var itemDrafts by remember { mutableStateOf(listOf<ExpenseItemDraft>()) }
    var shareDrafts by remember { mutableStateOf(listOf<ShareDraft>()) }

    val formValid = description.isNotBlank() &&
        paidBy.isNotBlank() &&
        (amount.toDoubleOrNull() ?: 0.0) > 0.0

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                enabled = formValid,
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    if (formValid) {
                        val expense = Expense(
                            tripId = tripId,
                            description = description,
                            amount = amountValue,
                            paidBy = paidBy,
                            category = category.ifBlank { null },
                            date = DateUtils.currentDateIso()
                        )

                        val expenseItems = itemDrafts.map { draft ->
                            ExpenseItem(
                                expenseId = expense.expenseId,
                                name = draft.name.ifBlank { "Item" },
                                amount = draft.amountText.toDoubleOrNull() ?: 0.0,
                                isLibre = draft.isLibre,
                                paidBy = draft.paidBy.ifBlank { null }
                            )
                        }

                        val finalShares = shareDrafts.mapNotNull { draft ->
                            val memberId = draft.memberId.trim()
                            val shareAmount = draft.amountText.toDoubleOrNull()
                            if (memberId.isBlank() || shareAmount == null || shareAmount <= 0) null
                            else ExpenseShare(
                                expenseId = expense.expenseId,
                                memberId = memberId,
                                finalAmount = shareAmount
                            )
                        }

                        onSaveExpense(expense, expenseItems, emptyList(), finalShares)

                        description = ""
                        amount = ""
                        category = ""
                        paidBy = ""
                        itemDrafts = emptyList()
                        shareDrafts = emptyList()
                    }
                }
            ) {
                Text("Save Expense")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Expense Details",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g., Lunch at Restaurant") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        placeholder = { Text("0.00") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        placeholder = { Text("Food") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = paidBy,
                    onValueChange = { paidBy = it },
                    label = { Text("Paid By (Member ID)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Items (Optional)",
                        style = MaterialTheme.typography.titleMedium
                    )

                    IconButton(onClick = {
                        itemDrafts = itemDrafts + ExpenseItemDraft()
                    }) {
                        Icon(Icons.Default.Add, "Add Item")
                    }
                }
            }

            items(itemDrafts, key = { it.id }) { draft ->
                ItemCard(
                    draft = draft,
                    onChange = { updated ->
                        itemDrafts = itemDrafts.map { if (it.id == updated.id) updated else it }
                    },
                    onDelete = { itemDrafts = itemDrafts.filterNot { it.id == draft.id } }
                )
            }

            item {
                ShareSection(
                    totalAmountInput = amount,
                    shareDrafts = shareDrafts,
                    onAddShare = { shareDrafts = shareDrafts + ShareDraft() },
                    onUpdateShare = { updated ->
                        shareDrafts = shareDrafts.map { if (it.id == updated.id) updated else it }
                    },
                    onRemoveShare = { toRemove ->
                        shareDrafts = shareDrafts.filterNot { it.id == toRemove.id }
                    }
                )
            }
        }
    }
}

@Composable
private fun ItemCard(
    draft: ExpenseItemDraft,
    onChange: (ExpenseItemDraft) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it)) },
                label = { Text("Item name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.amountText,
                onValueChange = { onChange(draft.copy(amountText = it)) },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.paidBy,
                onValueChange = { onChange(draft.copy(paidBy = it)) },
                label = { Text("Paid By (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = draft.isLibre,
                        onCheckedChange = { onChange(draft.copy(isLibre = it)) }
                    )
                    Text("Libre item?")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete Item")
                }
            }
        }
    }
}

@Composable
private fun ShareSection(
    totalAmountInput: String,
    shareDrafts: List<ShareDraft>,
    onAddShare: () -> Unit,
    onUpdateShare: (ShareDraft) -> Unit,
    onRemoveShare: (ShareDraft) -> Unit
) {
    val totalAmount = totalAmountInput.toDoubleOrNull()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Participants & Shares",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Enter member IDs and share amounts. Use equal split to auto-distribute the total.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (shareDrafts.isNotEmpty() && (totalAmount ?: 0.0) > 0) {
                TextButton(onClick = {
                    val perPerson = (totalAmount ?: 0.0) / shareDrafts.size.coerceAtLeast(1)
                    shareDrafts.forEach { draft ->
                        onUpdateShare(draft.copy(amountText = "%.2f".format(perPerson)))
                    }
                }) {
                    Text("Split equally")
                }
            }

            shareDrafts.forEach { draft ->
                ShareRow(
                    draft = draft,
                    onChange = onUpdateShare,
                    onRemove = { onRemoveShare(draft) }
                )
            }

            TextButton(onClick = onAddShare) {
                Text("Add participant")
            }
        }
    }
}

@Composable
private fun ShareRow(
    draft: ShareDraft,
    onChange: (ShareDraft) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft.memberId,
                onValueChange = { onChange(draft.copy(memberId = it)) },
                label = { Text("Member ID") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.amountText,
                onValueChange = { onChange(draft.copy(amountText = it)) },
                label = { Text("Share amount") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onRemove) {
                    Text("Remove")
                }
            }
        }
    }
}

private data class ExpenseItemDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val amountText: String = "",
    val isLibre: Boolean = false,
    val paidBy: String = ""
)

private data class ShareDraft(
    val id: String = UUID.randomUUID().toString(),
    val memberId: String = "",
    val amountText: String = ""
)
