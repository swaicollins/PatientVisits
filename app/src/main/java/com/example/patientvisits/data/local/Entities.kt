package com.example.patientvisits.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val patientId: String,
    val registrationDate: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val gender: String,
    val synced: Boolean = false,
    val remoteId: Int? = null
)

@Entity(
    tableName = "vitals",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["patientId"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["patientId", "visitDate"], unique = true)]
)
data class VitalsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: String,
    val visitDate: String,
    val heightCm: Double,
    val weightKg: Double,
    val bmi: Double,
    val synced: Boolean = false,
    val remoteId: Int? = null
)

@Entity(
    tableName = "assessments",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["patientId"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["patientId", "type", "visitDate"], unique = true)]
)
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: String,
    val type: String,
    val visitDate: String,
    val generalHealth: String,
    val answer: Boolean,
    val comments: String,
    val synced: Boolean = false
)
