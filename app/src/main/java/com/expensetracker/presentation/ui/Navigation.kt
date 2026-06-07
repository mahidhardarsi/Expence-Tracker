package com.expensetracker.presentation.ui

sealed class Screen(val route: String) {
    object ReportsList : Screen("reports_list")
    object ReportDetail : Screen("report_detail")
    object TransactionForm : Screen("transaction_form")
    object Analysis : Screen("analysis")
    object Options : Screen("options")
}

sealed class AppTab(val route: String, val label: String) {
    object Reports : AppTab("reports", "Reports")
    object Analysis : AppTab("analysis", "Analysis")
    object Options : AppTab("options", "Options")
}
