from django.db import models
from django.contrib.auth.models import User

class UserProfile(models.Model):
    ROLE_CHOICES = (
        ('ADMIN', 'Admin'),
        ('CASHIER', 'Cashier'),
        ('RECEPTIONIST', 'Receptionist'),
        ('STAFF', 'Staff'),
    )
    user = models.OneToOneField(User, on_delete=models.CASCADE)
    role = models.CharField(max_length=20, choices=ROLE_CHOICES, default='STAFF')

    def __str__(self):
        return f"{self.user.username} - {self.role}"

class InventoryItem(models.Model):
    name = models.CharField(max_length=100)
    quantity_in_stock = models.FloatField()
    unit = models.CharField(max_length=20, help_text="e.g., kg, liters, units")
    reorder_level = models.FloatField(default=10.0)

    def __str__(self):
        return f"{self.name} ({self.quantity_in_stock} {self.unit})"

class MenuItem(models.Model):
    name = models.CharField(max_length=100)
    description = models.TextField(blank=True, null=True)
    # FloatField instead of DecimalField — djongo/BSON cannot encode Python Decimal
    price = models.FloatField()
    category = models.CharField(max_length=50)
    is_available = models.BooleanField(default=True)

    def __str__(self):
        return self.name

class Table(models.Model):
    table_number = models.IntegerField(unique=True)
    capacity = models.IntegerField()
    is_occupied = models.BooleanField(default=False)

    def __str__(self):
        return f"Table {self.table_number} (Capacity: {self.capacity})"

class Reservation(models.Model):
    STATUS_CHOICES = (
        ('PENDING', 'Pending'),
        ('CONFIRMED', 'Confirmed'),
        ('CANCELLED', 'Cancelled'),
    )
    customer_name = models.CharField(max_length=100)
    contact_info = models.CharField(max_length=100)
    table_number = models.IntegerField()
    reservation_time = models.DateTimeField()
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='PENDING')

    def __str__(self):
        return f"{self.customer_name} - Table {self.table_number} at {self.reservation_time}"

class Order(models.Model):
    STATUS_CHOICES = (
        ('PENDING', 'Pending'),
        ('PREPARING', 'Preparing'),
        ('COMPLETED', 'Completed'),
        ('CANCELLED', 'Cancelled'),
    )
    ORDER_TYPE_CHOICES = (
        ('DINE_IN', 'Dine-In'),
        ('TAKEOUT', 'Takeout'),
        ('DELIVERY', 'Delivery'),
    )
    PAYMENT_METHOD_CHOICES = (
        ('CASH', 'Cash'),
        ('CARD', 'Card'),
        ('UPI', 'UPI / QR'),
    )
    PAYMENT_STATUS_CHOICES = (
        ('PAID', 'Paid'),
        ('UNPAID', 'Unpaid'),
    )

    table_number = models.IntegerField(null=True, blank=True)
    total_amount = models.FloatField(default=0.00)
    discount_amount = models.FloatField(default=0.00)
    discount_name = models.CharField(max_length=50, blank=True, null=True)
    order_type = models.CharField(max_length=20, choices=ORDER_TYPE_CHOICES, default='DINE_IN')
    payment_method = models.CharField(max_length=20, choices=PAYMENT_METHOD_CHOICES, default='CASH')
    payment_status = models.CharField(max_length=20, choices=PAYMENT_STATUS_CHOICES, default='PAID')
    customer_name = models.CharField(max_length=100, blank=True, null=True)
    customer_phone = models.CharField(max_length=20, blank=True, null=True)
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='PENDING')
    created_at = models.DateTimeField(auto_now_add=True)
    handled_by_username = models.CharField(max_length=100, blank=True, null=True)
    items_json = models.TextField(blank=True, null=True)

    def __str__(self):
        return f"Order #{self.id} ({self.order_type}) - {self.status}"

class OrderItem(models.Model):
    order_id = models.IntegerField()
    menu_item_name = models.CharField(max_length=100)
    quantity = models.IntegerField(default=1)
    price = models.FloatField(default=0.00)

    def __str__(self):
        return f"{self.quantity} x {self.menu_item_name}"
