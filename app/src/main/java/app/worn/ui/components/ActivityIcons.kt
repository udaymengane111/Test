package app.worn.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import app.worn.domain.model.DefaultActivities

fun activityIcon(iconKey: String, activityId: String? = null): ImageVector {
    return when (iconKey) {
        "tea" -> Icons.Outlined.LocalCafe
        "restaurant" -> Icons.Outlined.Restaurant
        "cookie" -> Icons.Outlined.BakeryDining
        "more" -> Icons.Outlined.MoreHoriz
        else -> when (activityId) {
            DefaultActivities.TEA -> Icons.Outlined.LocalCafe
            DefaultActivities.LUNCH -> Icons.Outlined.Restaurant
            DefaultActivities.SNACK -> Icons.Outlined.BakeryDining
            else -> Icons.Outlined.MoreHoriz
        }
    }
}
