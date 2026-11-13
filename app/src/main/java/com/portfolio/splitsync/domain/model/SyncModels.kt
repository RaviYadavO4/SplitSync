package com.portfolio.splitsync.domain.model

enum class SyncStatus {
    SYNCED,         // Cloud aur local dono identical hain
    PENDING_CREATE, // Offline naya item banaya gaya hai
    PENDING_UPDATE, // Offline edit kiya gaya hai
    PENDING_DELETE, // Offline delete mark kiya gaya hai (Soft Delete)
    CONFLICT        // Do users ne ek saath edit kiya hai, manual/auto resolution chahiye
}

enum class SplitType {
    EQUAL,
    EXACT,
    PERCENTAGE
}
