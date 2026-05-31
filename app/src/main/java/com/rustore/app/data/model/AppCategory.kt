package com.rustore.app.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppCategory(val displayName: String, val icon: ImageVector, val color: Color) {
    FINANCE("Финансы", Icons.Filled.AccountBalanceWallet, Color(0xFF4EB34E)),
    TOOLS("Инструменты", Icons.Filled.Menu, Color(0xFFF15D43)),
    GAMES("Игры", Icons.Filled.SportsEsports, Color(0xFF7A31C0)),
    GOVERNMENT("Государственные", Icons.Filled.AccountBalance, Color(0xFF0078FF)),
    TRANSPORT("Транспорт", Icons.Filled.DirectionsCar, Color(0xFFFFA100))
}

