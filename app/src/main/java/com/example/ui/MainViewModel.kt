package com.example.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.provider.Settings
import android.os.Build
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.json.JSONArray

enum class AppScreen {
    Dashboard, Money, Tasks, Site, More
}

data class GoogleUser(
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val idToken: String? = null,
    val isGuest: Boolean = false
)

class MainViewModel(private val repository: ConstructionRepository) : ViewModel() {

    // ==========================================
    // UI EVENT CHANNEL (replaces Context-based Toasts)
    // ==========================================
    private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    private fun emitEvent(event: UiEvent) {
        viewModelScope.launch { _uiEvents.emit(event) }
    }

    // ==========================================
    // OPERATION PROGRESS TRACKING
    // ==========================================
    var operationProgress by mutableStateOf(OperationProgress())
        private set

    // ==========================================
    // GOOGLE USER SESSION
    // ==========================================
    private val _userSession = MutableStateFlow<GoogleUser?>(null)
    val userSession: StateFlow<GoogleUser?> = _userSession.asStateFlow()

    // Unique device identifier
    var deviceId: String = ""
        private set

    // State flow for remote restrictions blocking
    private val _isUserBlocked = MutableStateFlow(false)
    val isUserBlocked: StateFlow<Boolean> = _isUserBlocked.asStateFlow()

    // Analytics instance
    private var firebaseAnalytics: FirebaseAnalytics? = null

    private fun getAnalytics(context: Context): FirebaseAnalytics? {
        if (firebaseAnalytics == null) {
            try {
                firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        return firebaseAnalytics
    }

    fun initDeviceAndRestrictions(context: Context) {
        try {
            if (deviceId.isEmpty()) {
                deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
            }
            checkRemoteRestrictions(context)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun trackUserLogin(context: Context, user: GoogleUser) {
        try {
            val fa = getAnalytics(context) ?: return
            val deviceModel = Build.MODEL
            val deviceManufacturer = Build.MANUFACTURER

            // Set User Properties
            fa.setUserId(user.email)
            fa.setUserProperty("user_email", user.email)
            fa.setUserProperty("user_name", user.displayName)
            fa.setUserProperty("device_id", deviceId)

            // Log Event
            val bundle = android.os.Bundle().apply {
                putString("user_email", user.email)
                putString("user_name", user.displayName)
                putString("device_id", deviceId)
                putString("device_model", "$deviceManufacturer $deviceModel")
                putString("login_timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            }
            fa.logEvent("login_success", bundle)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun trackDeviceRegistration(context: Context) {
        try {
            val fa = getAnalytics(context) ?: return
            val deviceModel = Build.MODEL
            val deviceManufacturer = Build.MANUFACTURER

            val bundle = android.os.Bundle().apply {
                putString("device_id", deviceId)
                putString("device_model", "$deviceManufacturer $deviceModel")
                putString("registered_timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            }
            fa.logEvent("device_registered", bundle)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun checkRemoteRestrictions(context: Context) {
        try {
            if (deviceId.isEmpty()) {
                deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
            }
        } catch (t: Throwable) {
            deviceId = "fallback_id"
        }
        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (com.example.BuildConfig.DEBUG) 0 else 3600)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)
            
            // Set default values
            val defaults = mapOf("restricted_identities" to "[]")
            remoteConfig.setDefaultsAsync(defaults)

            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                val restrictedIdentitiesJson = remoteConfig.getString("restricted_identities")
                processRestrictions(context, restrictedIdentitiesJson)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun processRestrictions(context: Context, restrictedJson: String) {
        val restrictedList = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(restrictedJson)
            for (i in 0 until jsonArray.length()) {
                val value = jsonArray.getString(i).trim().lowercase()
                if (value.isNotEmpty() && value != "null" && value != "[]" && value != "fallback_id") {
                    restrictedList.add(value)
                }
            }
        } catch (e: Exception) {
            // Fallback: parse as comma-separated
            restrictedJson.split(",").forEach {
                val value = it.trim().lowercase()
                if (value.isNotEmpty() && value != "null" && value != "[]" && value != "fallback_id") {
                    restrictedList.add(value)
                }
            }
        }

        val currentEmail = _userSession.value?.email?.trim()?.lowercase() ?: ""
        val currentDeviceId = deviceId.trim().lowercase()

        // Explicit security safeguards to prevent accidental system locking on emulators/sandboxes
        val isBlocked = if (currentEmail == "demo.contractor@constructpro.net") {
            false
        } else {
            val emailBlocked = currentEmail.isNotEmpty() &&
                    currentEmail != "null" &&
                    currentEmail != "fallback_id" &&
                    restrictedList.contains(currentEmail)

            val deviceBlocked = currentDeviceId.isNotEmpty() &&
                    currentDeviceId != "null" &&
                    currentDeviceId != "fallback_id" &&
                    currentDeviceId != "0000000000000000" &&
                    restrictedList.contains(currentDeviceId)

            emailBlocked || deviceBlocked
        }

        if (isBlocked) {
            _isUserBlocked.value = true
            // Clear credentials from preferences
            handleGoogleSignOut(context)
        } else {
            _isUserBlocked.value = false
        }
    }

    fun handleGoogleSignIn(user: GoogleUser, context: Context) {
        _userSession.value = user
        // Persist session
        val prefs = context.getSharedPreferences("constructpro_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("auth_name", user.displayName)
        editor.putString("auth_email", user.email)
        editor.putString("auth_photo", user.photoUrl ?: "")
        editor.putBoolean("auth_guest", user.isGuest)
        editor.apply()
        checkAndSeedDemoData()
    }

    fun handleGoogleSignOut(context: Context) {
        _userSession.value = null
        val prefs = context.getSharedPreferences("constructpro_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .remove("auth_name")
            .remove("auth_email")
            .remove("auth_photo")
            .remove("auth_token")
            .remove("auth_guest")
            .apply()
    }

    fun loadUserSessionFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences("constructpro_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString("auth_name", null)
        val email = prefs.getString("auth_email", null)
        if (name != null && email != null) {
            val photo = prefs.getString("auth_photo", "") ?: ""
            val isGuest = prefs.getBoolean("auth_guest", false)
            _userSession.value = GoogleUser(
                displayName = name,
                email = email,
                photoUrl = if (photo.isEmpty()) null else photo,
                idToken = null,
                isGuest = isGuest
            )
            checkAndSeedDemoData()
        } else {
            _userSession.value = null
        }
    }

    private fun checkAndSeedDemoData() {
        val user = _userSession.value
        if (user != null && user.email.trim().lowercase() == "demo.contractor@constructpro.net") {
            viewModelScope.launch {
                val currentProjects = repository.allProjects.firstOrNull() ?: emptyList()
                if (currentProjects.isEmpty()) {
                    seedComprehensiveDemoData()
                }
            }
        }
    }

    private suspend fun seedComprehensiveDemoData() {
        // 1. Seed Projects
        val p1Id_long = repository.insertProject(Project(
            name = "Pune Highway Interchange",
            location = "NH-4 Bypass Sector 4",
            budget = 2500000.0,
            status = "Active"
        ))
        val p2Id_long = repository.insertProject(Project(
            name = "Emerald Commercial Plaza",
            location = "MG Road Galleria District",
            budget = 8000000.0,
            status = "Active"
        ))
        val p3Id_long = repository.insertProject(Project(
            name = "Galaxy Residential Tower",
            location = "Sector 75 Tech Hub",
            budget = 15000000.0,
            status = "On Hold"
        ))

        val p1Id = p1Id_long.toInt()
        val p2Id = p2Id_long.toInt()
        val p3Id = p3Id_long.toInt()

        // 2. Seed Workers
        val w1Id_long = repository.insertWorker(Worker(
            name = "Vikram Singh",
            role = "Site Supervisor",
            shift = "Day",
            wageRate = 850.0,
            avatarColor = 0xFF1976D2.toInt(),
            phone = "9876543210",
            email = "vikram@constructpro.net"
        ))
        val w2Id_long = repository.insertWorker(Worker(
            name = "Amit Sharma",
            role = "Senior Carpenter",
            shift = "Day",
            wageRate = 650.0,
            avatarColor = 0xFF388E3C.toInt(),
            phone = "9876543211",
            email = "amit.carpenter@gmail.com"
        ))
        val w3Id_long = repository.insertWorker(Worker(
            name = "Suresh Patil",
            role = "Mason Craftsman",
            shift = "Night",
            wageRate = 600.0,
            avatarColor = 0xFF7B1FA2.toInt(),
            phone = "9876543212",
            email = "suresh.mason@gmail.com"
        ))
        val w4Id_long = repository.insertWorker(Worker(
            name = "Rohan Das",
            role = "Skilled Welder",
            shift = "Day",
            wageRate = 700.0,
            avatarColor = 0xFFF57C00.toInt(),
            phone = "9876543213",
            email = "rohan.weld@gmail.com"
        ))
        val w5Id_long = repository.insertWorker(Worker(
            name = "Karan Johar",
            role = "Project Architect",
            shift = "Day",
            wageRate = 1500.0,
            avatarColor = 0xFF0097A7.toInt(),
            phone = "9876543214",
            email = "karan.arch@gmail.com"
        ))

        val w1Id = w1Id_long.toInt()
        val w2Id = w2Id_long.toInt()
        val w3Id = w3Id_long.toInt()
        val w4Id = w4Id_long.toInt()
        val w5Id = w5Id_long.toInt()

        // 3. Seed Leads
        repository.insertLead(Lead(
            clientName = "Rajesh Singhal",
            clientPhone = "+91 9988776655",
            clientEmail = "rajesh@singhalmetals.com",
            projectType = "Warehouse Block C",
            budget = 4500000.0,
            status = "New",
            dateCreated = "2026-06-01",
            notes = "Prefers steel structure. Site is clear. Wants quote by end of week."
        ))
        repository.insertLead(Lead(
            clientName = "Dr. Anjali Mehta",
            clientPhone = "+91 9112233445",
            clientEmail = "dr.mehta@dentalcare.in",
            projectType = "Luxury Villa",
            budget = 12000000.0,
            status = "Contacted",
            dateCreated = "2026-05-28",
            notes = "Architectural drawings finalized. Needs high-end Italian marble specifications."
        ))
        repository.insertLead(Lead(
            clientName = "Prestige Developers",
            clientPhone = "+91 8080808080",
            clientEmail = "bids@prestigegroup.in",
            projectType = "Commercial Complex",
            budget = 35000000.0,
            status = "Quoted",
            dateCreated = "2026-05-20",
            notes = "Submitted detailed structural tender. Awaiting selection committee decision."
        ))
        repository.insertLead(Lead(
            clientName = "Sanjay Dutt",
            clientPhone = "+91 9654321777",
            clientEmail = "sanjay@duttheavy.com",
            projectType = "Farmhouse Renovation",
            budget = 1800000.0,
            status = "Converted",
            dateCreated = "2026-05-15",
            notes = "Converted to active project. Fully integrated into active list."
        ))
        repository.insertLead(Lead(
            clientName = "Kapil Dev",
            clientPhone = "+91 9898989898",
            clientEmail = "kapil@crickethaven.com",
            projectType = "Sports Clinic Center",
            budget = 6700000.0,
            status = "Lost",
            dateCreated = "2026-05-10",
            notes = "Client chose cheaper pre-fab alternative due to budget constraints."
        ))

        // 4. Seed Tasks
        // Pune Highway (p1Id)
        repository.insertTask(Task(
            projectId = p1Id,
            title = "Foundation Excavation",
            priority = "High",
            status = "Done",
            dueDate = "2026-06-05",
            assignee = "Suresh Patil"
        ))
        repository.insertTask(Task(
            projectId = p1Id,
            title = "Concrete Pouring Bridge Pier",
            priority = "High",
            status = "In Progress",
            dueDate = "2026-06-12",
            assignee = "Vikram Singh"
        ))
        repository.insertTask(Task(
            projectId = p1Id,
            title = "Structural Steel Welding",
            priority = "Medium",
            status = "To Do",
            dueDate = "2026-06-25",
            assignee = "Rohan Das"
        ))

        // Emerald Plaza (p2Id)
        repository.insertTask(Task(
            projectId = p2Id,
            title = "Slab Shuttering Level 1",
            priority = "High",
            status = "In Progress",
            dueDate = "2026-06-18",
            assignee = "Amit Sharma"
        ))
        repository.insertTask(Task(
            projectId = p2Id,
            title = "Electrical Conduit Laying",
            priority = "Medium",
            status = "To Do",
            dueDate = "2026-06-28",
            assignee = "Karan Johar"
        ))

        // Galaxy Tower (p3Id)
        repository.insertTask(Task(
            projectId = p3Id,
            title = "Soil Bearing Capacity Test",
            priority = "Low",
            status = "Done",
            dueDate = "2026-05-15",
            assignee = "Karan Johar"
        ))

        // 5. Seed Transactions (Revenue vs Expense)
        // Pune Highway (p1Id)
        repository.insertTransaction(Transaction(
            projectId = p1Id,
            type = "Money In",
            amount = 1200000.0,
            category = "Client Advance",
            description = "First milestone release foundation complete",
            date = "2026-06-01",
            paymentMethod = "Bank Transfer",
            reference = "TXN82718105"
        ))
        repository.insertTransaction(Transaction(
            projectId = p1Id,
            type = "Money Out",
            amount = 350000.0,
            category = "Material",
            description = "High strength cement and sand supply",
            date = "2026-06-02",
            paymentMethod = "Cheque",
            reference = "CHQ_CEMENT_99"
        ))
        repository.insertTransaction(Transaction(
            projectId = p1Id,
            type = "Money Out",
            amount = 150000.0,
            category = "Equipment",
            description = "JCB excavator weekly hire charge",
            date = "2026-06-04",
            paymentMethod = "Cash",
            reference = "REF_JCB_RENTAL"
        ))
        repository.insertTransaction(Transaction(
            projectId = p1Id,
            type = "Money Out",
            amount = 50000.0,
            category = "Labor",
            description = "Weekly casual labor wage release",
            date = "2026-06-06",
            paymentMethod = "Cash",
            reference = "W_WEEK1"
        ))

        // Emerald Plaza (p2Id)
        repository.insertTransaction(Transaction(
            projectId = p2Id,
            type = "Money In",
            amount = 2500000.0,
            category = "Client Advance",
            description = "Mobilization fund and layout clearance",
            date = "2026-05-25",
            paymentMethod = "Bank Transfer",
            reference = "MOB_991823"
        ))
        repository.insertTransaction(Transaction(
            projectId = p2Id,
            type = "Money Out",
            amount = 620000.0,
            category = "Material",
            description = "Structural steel reinforcement rebar",
            date = "2026-05-28",
            paymentMethod = "Bank Transfer",
            reference = "UT_REBAR_99"
        ))
        repository.insertTransaction(Transaction(
            projectId = p2Id,
            type = "Money Out",
            amount = 180000.0,
            category = "Other",
            description = "Municipal structural approval license",
            date = "2026-05-30",
            paymentMethod = "Bank Transfer",
            reference = "MNC_LIC_20"
        ))

        // 6. Minutes of Meetings (MOM)
        repository.insertMOM(MOM(
            projectId = p1Id,
            title = "NH-4 Pier Alignment Correction meeting",
            content = "Met with Senior Government Highway Consultant regarding Pier 4 vertical tilt deviation. Agreed to add lateral reinforcement bracing plates. Additional cost of ₹1.2L approved dynamically. Next inspection set for Thursday.",
            date = "2026-06-04"
        ))
        repository.insertMOM(MOM(
            projectId = p1Id,
            title = "Weekly Construction Materials Sync",
            content = "Reviewed rebar steel stock levels. Current inventory sufficient for 10 days of continuous casting. Vikram to coordinate next bulk truck dispatch from factory. Approved minor rate correction for specialized welders.",
            date = "2026-06-05"
        ))

        // 7. Attendance logs (last 3 days)
        val dates = listOf("2026-06-05", "2026-06-06", "2026-06-07")
        dates.forEach { date ->
            repository.insertAttendance(Attendance(
                workerId = w1Id,
                projectId = p1Id,
                date = date,
                status = "Present",
                overtimeHours = 1.0
            ))
            repository.insertAttendance(Attendance(
                workerId = w2Id,
                projectId = p1Id,
                date = date,
                status = "Present",
                overtimeHours = 0.0
            ))
            repository.insertAttendance(Attendance(
                workerId = w3Id,
                projectId = p1Id,
                date = date,
                status = "Present",
                overtimeHours = 2.0
            ))
            repository.insertAttendance(Attendance(
                workerId = w4Id,
                projectId = p1Id,
                date = date,
                status = if (date == "2026-06-06") "Absent" else "Present",
                overtimeHours = if (date == "2026-06-06") 0.0 else 1.5
            ))
        }

        // 8. Payroll history
        repository.insertPayroll(Payroll(
            workerId = w1Id,
            projectId = p1Id,
            date = "2026-06-06",
            wagesPaid = 1700.0,
            status = "Paid"
        ))
        repository.insertPayroll(Payroll(
            workerId = w3Id,
            projectId = p1Id,
            date = "2026-06-06",
            wagesPaid = 1200.0,
            status = "Paid"
        ))
        repository.insertPayroll(Payroll(
            workerId = w2Id,
            projectId = p1Id,
            date = "2026-06-07",
            wagesPaid = 650.0,
            status = "Pending"
        ))

        // 9. Quotations / Estimates
        // Pune Highway (p1Id)
        repository.insertEstimate(Estimate(
            projectId = p1Id,
            itemName = "Heavy Site Earth Excavation and Grading",
            quantity = 120.0,
            unit = "Hrs",
            rate = 1200.0,
            totalCost = 144000.0
        ))
        repository.insertEstimate(Estimate(
            projectId = p1Id,
            itemName = "M25 Reinforcement Cement Concrete Mix",
            quantity = 250.0,
            unit = "CuM",
            rate = 4500.0,
            totalCost = 1125000.0
        ))
        repository.insertEstimate(Estimate(
            projectId = p1Id,
            itemName = "Fe550 Structural Grade TMT Rebars",
            quantity = 12.0,
            unit = "Tons",
            rate = 58000.0,
            totalCost = 696000.0
        ))

        // Emerald Plaza (p2Id)
        repository.insertEstimate(Estimate(
            projectId = p2Id,
            itemName = "Premium Glazed Facade Double Seal Panels",
            quantity = 36.0,
            unit = "SqM",
            rate = 12500.0,
            totalCost = 450000.0
        ))
        repository.insertEstimate(Estimate(
            projectId = p2Id,
            itemName = "Level 1 Structural Deck Slab Concrete Cast",
            quantity = 850.0,
            unit = "SqFt",
            rate = 350.0,
            totalCost = 297500.0
        ))
    }

    // ==========================================
    // DATE UTILITIES
    // ==========================================
    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Returns today's date as YYYY-MM-DD string */
    fun todayIso(): String = isoFormatter.format(Date())

    // ==========================================
    // UI STATES
    // ==========================================
    var currentScreen by mutableStateOf(AppScreen.Dashboard)
    var selectedProjectId by mutableStateOf<Int?>(null) // Dynamic first project selector
    var attendanceDate by mutableStateOf(isoFormatter.format(Date())) // Date navigator — uses live date
    var darkThemeEnabled by mutableStateOf(false) // Premium dark glassmorphism mode toggle
    var transactionToEdit by mutableStateOf<Transaction?>(null)
    var activeSiteTab by mutableStateOf("Party")
    var onlineCloudLinkEnabled by mutableStateOf(true)

    // Dialogue triggers accessible globally across composing widgets
    var showQuickDialog by mutableStateOf(false)
    var showProjectDialog by mutableStateOf(false)
    var showTransactionDialog by mutableStateOf(false)
    var showTaskDialog by mutableStateOf(false)
    var showWorkerDialog by mutableStateOf(false)
    var transactionTypePreset by mutableStateOf("Money Out") // "Money In" or "Money Out"

    // Filtering/Search States
    var transactionSearchQuery by mutableStateOf("")
    var transactionTypeFilter by mutableStateOf("All") // "All", "Money In", "Money Out"
    var transactionCategoryFilter by mutableStateOf("All") // "All", "Material", "Labor", "Equipment", etc.

    var taskStatusFilter by mutableStateOf("All") // "All", "To Do", "In Progress", "Done"

    // Selected transaction state globally shared for beautiful click details navigation
    var sharedSelectedTxDetails by mutableStateOf<Transaction?>(null)

    // ==========================================
    // BASE DATABASE FLOWS
    // ==========================================
    val projects = repository.allProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val workers = repository.allWorkers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val attendance = repository.allAttendance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tasks = repository.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val moms = repository.allMOMs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val payroll = repository.allPayroll.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val estimates = repository.allEstimates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val leads = repository.allLeads.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Project
    val activeProject: StateFlow<Project?> = combine(projects, snapshotFlow { selectedProjectId }) { projectList: List<Project>, selectedId: Int? ->
        if (selectedId == null) projectList.firstOrNull()
        else projectList.find { it.id == selectedId } ?: projectList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Set selected project automatically if first project loads and selected is null
    init {
        viewModelScope.launch {
            projects.collectLatest { projectList ->
                if (selectedProjectId == null && projectList.isNotEmpty()) {
                    selectedProjectId = projectList.first().id
                }
            }
        }
    }

    // ==========================================
    // CRUD DATA MUTATORS (with error handling)
    // ==========================================

    // Projects
    fun addProject(
        name: String, 
        location: String, 
        budget: Double, 
        customBackground: String? = null,
        startDate: String = "",
        endDate: String = ""
    ) {
        val nameResult = FormValidator.validateProjectName(name)
        val locResult = FormValidator.validateLocation(location)
        if (!nameResult.isValid) { emitEvent(UiEvent.ShowToast(nameResult.errorMessage ?: "Invalid name")); return }
        if (!locResult.isValid) { emitEvent(UiEvent.ShowToast(locResult.errorMessage ?: "Invalid location")); return }

        viewModelScope.launch {
            try {
                val id = repository.insertProject(
                    Project(
                        name = name, 
                        location = location, 
                        budget = budget, 
                        status = "Active", 
                        customBackground = customBackground,
                        startDate = startDate,
                        endDate = endDate
                    )
                )
                selectedProjectId = id.toInt()
                emitEvent(UiEvent.ShowToast("Project \"$name\" created!"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to create project", e))
            }
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            try {
                repository.updateProject(project)
                emitEvent(UiEvent.ShowToast("Project updated successfully"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to update project", e))
            }
        }
    }

    fun deleteProject(project: Project, context: Context) {
        viewModelScope.launch {
            try {
                repository.deleteProject(project)
                emitEvent(UiEvent.ShowToast("Project deleted successfully"))
                // Reset project focus
                selectedProjectId = null
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to delete project", e))
            }
        }
    }

    // Workers
    fun addWorker(
        name: String,
        role: String,
        shift: String,
        wageRate: Double,
        color: Int,
        phone: String = "",
        email: String = "",
        partyType: String = "Worker",
        address: String = "",
        partyId: String = "",
        dateOfJoining: String = "",
        aadhaar: String = "",
        pan: String = "",
        reference: String = ""
    ) {
        val nameResult = FormValidator.validateWorkerName(name)
        if (!nameResult.isValid) { emitEvent(UiEvent.ShowToast(nameResult.errorMessage ?: "Invalid name")); return }

        viewModelScope.launch {
            try {
                repository.insertWorker(
                    Worker(
                        name = name,
                        role = role,
                        shift = shift,
                        wageRate = wageRate,
                        avatarColor = color,
                        phone = phone,
                        email = email,
                        partyType = partyType,
                        address = address,
                        partyId = partyId,
                        dateOfJoining = dateOfJoining,
                        aadhaar = aadhaar,
                        pan = pan,
                        reference = reference
                    )
                )
                emitEvent(UiEvent.ShowToast("Party \"$name\" added!"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to add worker", e))
            }
        }
    }

    fun addWorkerInline(
        name: String,
        role: String,
        shift: String,
        wageRate: Double,
        color: Int,
        phone: String = "",
        email: String = "",
        partyType: String = "Worker",
        address: String = "",
        partyId: String = "",
        dateOfJoining: String = "",
        aadhaar: String = "",
        pan: String = "",
        reference: String = "",
        onSuccess: (Worker) -> Unit
    ) {
        val nameResult = FormValidator.validateWorkerName(name)
        if (!nameResult.isValid) { emitEvent(UiEvent.ShowToast(nameResult.errorMessage ?: "Invalid name")); return }

        viewModelScope.launch {
            try {
                val newWorker = Worker(
                    name = name,
                    role = role,
                    shift = shift,
                    wageRate = wageRate,
                    avatarColor = color,
                    phone = phone,
                    email = email,
                    partyType = partyType,
                    address = address,
                    partyId = partyId,
                    dateOfJoining = dateOfJoining,
                    aadhaar = aadhaar,
                    pan = pan,
                    reference = reference
                )
                val insertedId = repository.insertWorker(newWorker)
                val finalWorker = newWorker.copy(id = insertedId.toInt())
                emitEvent(UiEvent.ShowToast("Party \"$name\" created and selected!"))
                onSuccess(finalWorker)
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to add party inline", e))
            }
        }
    }

    fun updateWorker(worker: Worker) {
        viewModelScope.launch {
            try {
                repository.updateWorker(worker)
                emitEvent(UiEvent.ShowToast("Worker profile updated"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to update worker", e))
            }
        }
    }

    fun deleteWorker(worker: Worker, context: Context) {
        viewModelScope.launch {
            try {
                repository.deleteWorker(worker)
                emitEvent(UiEvent.ShowToast("Worker profile removed"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to delete worker", e))
            }
        }
    }

    // Attendance (Toggle/Mark)
    fun recordAttendance(workerId: Int, projectId: Int, date: String, status: String, overtimeHours: Double = 0.0) {
        viewModelScope.launch {
            try {
                if (status == "Clear") {
                    repository.deleteAttendanceRecord(workerId, date)
                } else {
                    repository.insertAttendance(
                        Attendance(
                            workerId = workerId,
                            projectId = projectId,
                            date = date,
                            status = status,
                            overtimeHours = overtimeHours
                        )
                    )
                }
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to record attendance", e))
            }
        }
    }

    // Tasks
    fun addTask(projectId: Int, title: String, priority: String, assignee: String, dueDate: String) {
        val titleResult = FormValidator.validateTaskTitle(title)
        if (!titleResult.isValid) { emitEvent(UiEvent.ShowToast(titleResult.errorMessage ?: "Invalid title")); return }

        viewModelScope.launch {
            try {
                repository.insertTask(
                    Task(
                        projectId = projectId,
                        title = title,
                        priority = priority,
                        status = "To Do",
                        dueDate = dueDate,
                        assignee = assignee
                    )
                )
                emitEvent(UiEvent.ShowToast("Task assigned!"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to create task", e))
            }
        }
    }

    fun cycleTaskStatus(task: Task) {
        viewModelScope.launch {
            try {
                val nextStatus = when (task.status) {
                    "To Do" -> "In Progress"
                    "In Progress" -> "Done"
                    else -> "To Do"
                }
                repository.updateTask(task.copy(status = nextStatus))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to update task", e))
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.updateTask(task)
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to update task", e))
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.deleteTask(task)
                emitEvent(UiEvent.ShowToast("Task deleted"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to delete task", e))
            }
        }
    }

    // Transactions
    fun addTransaction(
        projectId: Int,
        type: String,
        amount: Double,
        category: String,
        description: String,
        date: String,
        partyId: Int? = null,
        partyName: String? = null,
        reference: String = "",
        paymentMethod: String = "Cash"
    ) {
        if (amount <= 0.0) {
            emitEvent(UiEvent.ShowToast("Enter a valid transaction amount"))
            return
        }
        if (description.isBlank()) {
            emitEvent(UiEvent.ShowToast("Add a short transaction description"))
            return
        }
        viewModelScope.launch {
            try {
                repository.insertTransaction(
                    Transaction(
                        projectId = projectId,
                        type = type,
                        amount = amount,
                        category = category,
                        description = description,
                        date = date,
                        partyId = partyId,
                        partyName = partyName,
                        reference = reference,
                        paymentMethod = paymentMethod
                    )
                )
                emitEvent(UiEvent.ShowToast("Transaction recorded!"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to record transaction", e))
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transaction)
                emitEvent(UiEvent.ShowToast("Transaction deleted"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to delete transaction", e))
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.updateTransaction(transaction)
                emitEvent(UiEvent.ShowToast("Transaction updated!"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to update transaction", e))
            }
        }
    }

    // MOM
    fun addMOM(projectId: Int, title: String, content: String, date: String) {
        viewModelScope.launch {
            try {
                repository.insertMOM(MOM(projectId = projectId, title = title, content = content, date = date))
                emitEvent(UiEvent.ShowToast("Meeting minutes saved"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to save MOM", e))
            }
        }
    }

    fun deleteMOM(mom: MOM) {
        viewModelScope.launch {
            try {
                repository.deleteMOM(mom)
                emitEvent(UiEvent.ShowToast("Meeting minutes deleted"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to delete MOM", e))
            }
        }
    }

    // Payroll
    fun addPayroll(workerId: Int, projectId: Int, date: String, wagesPaid: Double, status: String) {
        if (wagesPaid <= 0.0) {
            emitEvent(UiEvent.ShowToast("Enter a valid payroll amount"))
            return
        }
        viewModelScope.launch {
            try {
                repository.insertPayroll(Payroll(workerId = workerId, projectId = projectId, date = date, wagesPaid = wagesPaid, status = status))
                emitEvent(UiEvent.ShowToast("Payroll entry recorded"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to process payroll", e))
            }
        }
    }

    fun updatePayroll(payroll: Payroll) {
        viewModelScope.launch {
            try {
                repository.updatePayroll(payroll)
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to update payroll", e))
            }
        }
    }

    fun deletePayroll(payroll: Payroll) {
        viewModelScope.launch {
            try {
                repository.deletePayroll(payroll)
                emitEvent(UiEvent.ShowToast("Payroll entry removed"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to delete payroll", e))
            }
        }
    }

    // Estimates
    fun addEstimate(projectId: Int, itemName: String, quantity: Double, unit: String, rate: Double) {
        if (itemName.isBlank()) {
            emitEvent(UiEvent.ShowToast("Enter an estimate item name"))
            return
        }
        if (quantity <= 0.0 || rate <= 0.0) {
            emitEvent(UiEvent.ShowToast("Quantity and rate must be greater than zero"))
            return
        }
        viewModelScope.launch {
            try {
                repository.insertEstimate(
                    Estimate(
                        projectId = projectId,
                        itemName = itemName,
                        quantity = quantity,
                        unit = unit,
                        rate = rate,
                        totalCost = quantity * rate
                    )
                )
                emitEvent(UiEvent.ShowToast("Estimate added"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to add estimate", e))
            }
        }
    }

    fun deleteEstimate(estimate: Estimate) {
        viewModelScope.launch {
            try {
                repository.deleteEstimate(estimate)
                emitEvent(UiEvent.ShowToast("Estimate removed"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to delete estimate", e))
            }
        }
    }

    fun updateEstimate(estimate: Estimate) {
        viewModelScope.launch {
            try {
                repository.updateEstimate(estimate)
                emitEvent(UiEvent.ShowToast("Estimate updated"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to update estimate", e))
            }
        }
    }

    // Leads Management
    fun addLead(clientName: String, clientPhone: String, clientEmail: String, projectType: String, budget: Double, notes: String) {
        if (clientName.isBlank()) {
            emitEvent(UiEvent.ShowToast("Client name is required"))
            return
        }
        viewModelScope.launch {
            try {
                val newLead = Lead(
                    clientName = clientName,
                    clientPhone = clientPhone,
                    clientEmail = clientEmail,
                    projectType = projectType,
                    budget = budget,
                    status = "New",
                    dateCreated = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    notes = notes
                )
                repository.insertLead(newLead)
                emitEvent(UiEvent.ShowToast("Lead created successfully!"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to add lead", e))
            }
        }
    }

    fun updateLead(lead: Lead) {
        viewModelScope.launch {
            try {
                repository.updateLead(lead)
                emitEvent(UiEvent.ShowToast("Lead updated"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to update lead", e))
            }
        }
    }

    fun deleteLead(lead: Lead) {
        viewModelScope.launch {
            try {
                repository.deleteLead(lead)
                emitEvent(UiEvent.ShowToast("Lead deleted"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to delete lead", e))
            }
        }
    }

    fun convertLeadToProject(lead: Lead) {
        viewModelScope.launch {
            try {
                val newProj = Project(
                    name = lead.clientName + " - " + lead.projectType,
                    location = if (lead.notes.isNotBlank()) lead.notes else "Client Contact: " + lead.clientPhone,
                    budget = lead.budget,
                    status = "Active",
                    customBackground = null
                )
                val pId = repository.insertProject(newProj)
                // Pre-populate project with a default estimate item matching the lead's budget estimates
                repository.insertEstimate(
                    Estimate(
                        projectId = pId.toInt(),
                        itemName = "Initial Estimated Cost for ${lead.projectType}",
                        quantity = 1.0,
                        unit = "Unit",
                        rate = lead.budget,
                        totalCost = lead.budget
                    )
                )
                repository.updateLead(lead.copy(status = "Converted"))
                selectedProjectId = pId.toInt()
                emitEvent(UiEvent.ShowToast("Lead successfully converted to Project!"))
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to convert lead to project", e))
            }
        }
    }

    // ==========================================
    // DATA EXPORT/IMPORT RIGS (with progress tracking)
    // ==========================================

    fun exportTransactionsCSV(context: Context) {
        viewModelScope.launch {
            try {
                operationProgress = OperationProgress(isActive = true, progress = 0.3f, message = "Preparing CSV...")
                val activeProj = activeProject.value ?: run {
                    emitEvent(UiEvent.ShowToast("No active project selected")); return@launch
                }
                val list = transactions.value.filter { it.projectId == activeProj.id }
                operationProgress = operationProgress.copy(progress = 0.7f, message = "Writing file...")
                DataIO.exportTransactionsCSV(context, list, activeProj.name)
                operationProgress = OperationProgress()
                emitEvent(UiEvent.ExportSuccess)
            } catch (e: Exception) {
                operationProgress = OperationProgress()
                emitEvent(UiEvent.ShowError("CSV export failed", e))
            }
        }
    }

    fun exportFullBackup(context: Context) {
        viewModelScope.launch {
            try {
                operationProgress = OperationProgress(isActive = true, progress = 0.2f, message = "Collecting data...")
                DataIO.exportBackupJSON(
                    context,
                    projects.value,
                    workers.value,
                    tasks.value,
                    transactions.value,
                    attendance.value,
                    moms.value,
                    payroll.value,
                    estimates.value,
                    leads.value
                )
                operationProgress = OperationProgress()
                emitEvent(UiEvent.ExportSuccess)
                emitEvent(UiEvent.ShowToast("Full backup exported!"))
            } catch (e: Exception) {
                operationProgress = OperationProgress()
                emitEvent(UiEvent.ShowError("Backup export failed", e))
            }
        }
    }

    fun importFullBackup(context: Context, jsonString: String) {
        viewModelScope.launch {
            try {
                operationProgress = OperationProgress(isActive = true, progress = 0.1f, message = "Parsing backup...")
                val success = DataIO.importBackupJSON(jsonString, AppDatabase.getDatabase(context).constructionDao())
                operationProgress = OperationProgress()
                if (success) {
                    emitEvent(UiEvent.ImportSuccess)
                    emitEvent(UiEvent.ShowToast("Database backup restored successfully!", long = true))
                } else {
                    emitEvent(UiEvent.ShowError("Failed to restore backup. Please verify file integrity."))
                }
            } catch (e: Exception) {
                operationProgress = OperationProgress()
                emitEvent(UiEvent.ShowError("Import failed: ${e.localizedMessage}", e))
            }
        }
    }

    // Project-wise backup & background mutators
    fun exportProjectBackup(context: Context, project: Project) {
        viewModelScope.launch {
            try {
                operationProgress = OperationProgress(isActive = true, progress = 0.3f, message = "Exporting project...")
                DataIO.exportProjectBackupJSON(
                    context,
                    project,
                    workers.value,
                    tasks.value,
                    transactions.value,
                    attendance.value,
                    moms.value,
                    payroll.value,
                    estimates.value
                )
                operationProgress = OperationProgress()
                emitEvent(UiEvent.ExportSuccess)
            } catch (e: Exception) {
                operationProgress = OperationProgress()
                emitEvent(UiEvent.ShowError("Project export failed", e))
            }
        }
    }

    fun importProjectBackup(context: Context, jsonString: String) {
        viewModelScope.launch {
            try {
                operationProgress = OperationProgress(isActive = true, progress = 0.1f, message = "Importing project...")
                val success = DataIO.importProjectBackupJSON(jsonString, AppDatabase.getDatabase(context).constructionDao())
                operationProgress = OperationProgress()
                if (success) {
                    emitEvent(UiEvent.ImportSuccess)
                    emitEvent(UiEvent.ShowToast("Project restored successfully!", long = true))
                } else {
                    emitEvent(UiEvent.ShowError("Failed to restore project backup. Check file integrity."))
                }
            } catch (e: Exception) {
                operationProgress = OperationProgress()
                emitEvent(UiEvent.ShowError("Project import failed", e))
            }
        }
    }

    fun updateProjectBackground(project: Project, backgroundStyle: String) {
        viewModelScope.launch {
            try {
                val updated = project.copy(customBackground = backgroundStyle)
                repository.updateProject(updated)
            } catch (e: Exception) {
                emitEvent(UiEvent.ShowError("Failed to update background", e))
            }
        }
    }

    // ==========================================
    // VIEW MODEL FACTORY DEFINITION
    // ==========================================
    class Factory(private val repository: ConstructionRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
