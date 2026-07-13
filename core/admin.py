from django.contrib import admin
from django.utils.html import format_html
from .models import UserProfile, InventoryItem, MenuItem, Table, Reservation, Order, OrderItem


# ─── BULK ACTIONS ─────────────────────────────────────────

def mark_completed(modeladmin, request, queryset):
    queryset.update(status='COMPLETED')
mark_completed.short_description = "Mark selected orders as Completed"

def mark_cancelled(modeladmin, request, queryset):
    queryset.update(status='CANCELLED')
mark_cancelled.short_description = "Mark selected orders as Cancelled"

def mark_preparing(modeladmin, request, queryset):
    queryset.update(status='PREPARING')
mark_preparing.short_description = "Mark selected orders as Preparing"


# ─── MODEL ADMINS ─────────────────────────────────────────

@admin.register(UserProfile)
class UserProfileAdmin(admin.ModelAdmin):
    list_display = ('user', 'role')
    list_filter = ('role',)

@admin.register(InventoryItem)
class InventoryItemAdmin(admin.ModelAdmin):
    list_display = ('name', 'quantity_in_stock', 'unit', 'reorder_level', 'stock_status')
    search_fields = ('name',)
    list_filter = ('unit',)

    def stock_status(self, obj):
        if obj.quantity_in_stock <= obj.reorder_level:
            return format_html('<span style="color: #ef4444; font-weight: bold;">LOW STOCK</span>')
        return format_html('<span style="color: #10b981; font-weight: bold;">OK</span>')
    stock_status.short_description = 'Status'

@admin.register(MenuItem)
class MenuItemAdmin(admin.ModelAdmin):
    list_display = ('name', 'category', 'formatted_price', 'is_available')
    list_filter = ('category', 'is_available')
    search_fields = ('name', 'description')
    list_editable = ('is_available',)

    def formatted_price(self, obj):
        return format_html('<strong>${}</strong>', f"{obj.price:.2f}")
    formatted_price.short_description = 'Price'

@admin.register(Table)
class TableAdmin(admin.ModelAdmin):
    list_display = ('table_number', 'capacity', 'occupancy_status')
    list_filter = ('is_occupied',)

    def occupancy_status(self, obj):
        if obj.is_occupied:
            return format_html('<span style="color: #ef4444; font-weight: bold;">OCCUPIED</span>')
        return format_html('<span style="color: #10b981; font-weight: bold;">AVAILABLE</span>')
    occupancy_status.short_description = 'Status'

@admin.register(Reservation)
class ReservationAdmin(admin.ModelAdmin):
    list_display = ('customer_name', 'table_number', 'reservation_time', 'status')
    list_filter = ('status', 'reservation_time')
    search_fields = ('customer_name',)

@admin.register(Order)
class OrderAdmin(admin.ModelAdmin):
    list_display = ('id', 'channel_badge', 'table_number', 'ordered_items', 'formatted_total', 'payment_badge', 'order_status', 'created_at')
    list_filter = ('order_type', 'payment_method', 'payment_status', 'status', 'created_at')
    search_fields = ('customer_name', 'customer_phone', 'handled_by_username')
    actions = [mark_completed, mark_cancelled, mark_preparing]

    def channel_badge(self, obj):
        badges = {
            'DINE_IN': ('#6366f1', '🍽️ Dine-In'),
            'TAKEOUT': ('#8b5cf6', '🛍️ Takeout'),
            'DELIVERY': ('#ec4899', '🚚 Delivery'),
        }
        color, label = badges.get(obj.order_type, ('#64748b', obj.order_type or 'Dine-In'))
        return format_html('<span style="background-color: {}; color: white; padding: 3px 8px; border-radius: 4px; font-weight: bold; font-size: 11px;">{}</span>', color, label)
    channel_badge.short_description = 'Channel'

    def payment_badge(self, obj):
        methods = {
            'CASH': '💵 Cash',
            'CARD': '💳 Card',
            'UPI': '📱 UPI / QR',
        }
        method_str = methods.get(obj.payment_method, obj.payment_method or 'Cash')
        status_color = '#10b981' if obj.payment_status == 'PAID' else '#ef4444'
        return format_html('<span>{}</span> <span style="color: {}; font-weight: bold; font-size: 11px;">[{}]</span>', method_str, status_color, obj.payment_status or 'PAID')
    payment_badge.short_description = 'Payment'

    def ordered_items(self, obj):
        import json
        if not obj.items_json:
            return "No items"
        try:
            items = json.loads(obj.items_json)
            html = '<ul style="margin: 0; padding-left: 15px; font-size: 12px;">'
            for item in items:
                html += f"<li>{item['quantity']}x <strong>{item['name']}</strong> (${item['price']:.2f})</li>"
            html += "</ul>"
            return format_html(html)
        except Exception:
            return obj.items_json
    ordered_items.short_description = 'Ordered Items'

    def formatted_total(self, obj):
        disc = f" <small style='color:#ef4444;'>(-${obj.discount_amount:.2f})</small>" if obj.discount_amount and obj.discount_amount > 0 else ""
        return format_html('<strong>${}</strong>{}', f"{obj.total_amount:.2f}", format_html(disc))
    formatted_total.short_description = 'Total'

    def order_status(self, obj):
        colors = {
            'PENDING': '#f59e0b',
            'PREPARING': '#3b82f6',
            'COMPLETED': '#10b981',
            'CANCELLED': '#ef4444',
        }
        color = colors.get(obj.status, '#64748b')
        return format_html('<span style="color: {}; font-weight: bold;">{}</span>', color, obj.status)
    order_status.short_description = 'Status'

@admin.register(OrderItem)
class OrderItemAdmin(admin.ModelAdmin):
    list_display = ('order_id', 'menu_item_name', 'quantity', 'formatted_price')
    list_filter = ('menu_item_name',)

    def formatted_price(self, obj):
        return format_html('${}', f"{obj.price:.2f}")
    formatted_price.short_description = 'Price'


# ─── SITE CUSTOMIZATION ──────────────────────────────────

admin.site.site_header = 'RMS Dashboard'
admin.site.site_title = 'RMS Admin'
admin.site.index_title = 'Restaurant Management System'
