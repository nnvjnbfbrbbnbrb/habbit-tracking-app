package com.ansangha.craxxjxbdbf.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ansangha.craxxjxbdbf.ui.theme.*
import com.ansangha.craxxjxbdbf.ui.viewmodel.HabitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    viewModel: com.ansangha.craxxjxbdbf.ui.viewmodel.HabitViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("🎯") }
    var selectedColor by remember { mutableStateOf("#FF6B6B") }
    var selectedCategory by remember { mutableStateOf("health") }
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(    Brush.Companion.background())
    ) {
        // Premium background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.1f }
                .background(Brush.Companion.primary())
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Premium Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Create Habit | Nilesh",
                    style = Typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Main Configuration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(24.dp, RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Input Fields
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Habit Name *") },
                        placeholder = { Text("e.g., Morning Run") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        placeholder = { Text("Why do you want this habit?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    // Icon & Color Selection with Premium Styling
                    Text(
                        text = "Customize",
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Icon Row
                    val habitIcons = listOf("🎯", "💪", "📚", "🧘", "🏃", "💧", "🎨", "🎵", "📝", "🌅")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(habitIcons) { icon ->
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .shadow(if (selectedIcon == icon) 8.dp else 2.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (selectedIcon == icon) MaterialTheme.colorScheme.primaryContainer 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable { selectedIcon = icon },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = icon, fontSize = 28.sp)
                            }
                        }
                    }

                    // Color Row
                    val colors = listOf("#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(colors) { colorHex ->
                            val color = Color(android.graphics.Color.parseColor(colorHex))
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .shadow(if (selectedColor == colorHex) 8.dp else 2.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .clickable { selectedColor = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == colorHex) {
                                    Icon(Icons.Default.Check, null, tint = Color.White)
                                }
                            }
                        }
                    }

                    // Category Selection
                    val categories = listOf("health", "fitness", "learning", "mindfulness", "productivity", "creative")
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(100.dp)
                    ) {
                        items(categories) { category ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategory = category },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCategory == category) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = category.replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = Typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // Save Button with Premium Gradient
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addHabit(name, description, selectedIcon, selectedColor, selectedCategory)
                        showDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(top = 16.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = name.isNotBlank()
            ) {
                Text(
                    text = "Launch Nilesh's Habit ✨",
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Success Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("🚀 Habit Launched!", fontWeight = FontWeight.Bold) },
                text = { Text("Nilesh, your new habit is live. Track it to build greatness!") },
                confirmButton = {
                    TextButton(onClick = {
                        showDialog = false
                        onSave()
                    }) {
                        Text("Let's Go!")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}