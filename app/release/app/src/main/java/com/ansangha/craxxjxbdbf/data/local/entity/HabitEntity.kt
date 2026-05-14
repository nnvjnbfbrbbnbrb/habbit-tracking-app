package com.ansangha.craxxjxbdbf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val targetCount: Int,
    val currentCount: Int,
    val icon: String, // emoji or icon name
    val color: String, // hex color
    val frequency: String, // daily, weekly, custom
    val isCompleted: Boolean,
    val streakDays: Int,
    val bestStreak: Int,
    val createdAt: Long,
    val lastCompletedDate: Long,
    val reminderTime: String, // HH:mm format
    val isActive: Boolean,
    val category: String, // health, fitness, learning, etc.
    val difficulty: String, // easy, medium, hard
    val priority: Int, // 1-5 importance level
    val completedDates: List<Long> // stored as JSON string
)

@Entity(tableName = "habit_completions")
data class HabitCompletionEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val completedAt: Long,
    val notes: String,
    val mood: Int, // 1-5 rating
    val duration: Long, // time taken in minutes
    val quality: String // excellent, good, fair, poor
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val category: String,
    val isUnlocked: Boolean,
    val unlockedAt: Long?,
    val progress: Int, // 0-100
    val requirementValue: Int,
    val currentProgress: Int
)