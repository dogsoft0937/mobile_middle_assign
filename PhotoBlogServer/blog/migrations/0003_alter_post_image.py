# Generated by Django 5.1.2 on 2024-10-24 07:23

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('blog', '0002_post_image'),
    ]

    operations = [
        migrations.AlterField(
            model_name='post',
            name='image',
            field=models.ImageField(default='blog_image/default_error.png', upload_to='blog_image/%Y/%m/%d/'),
        ),
    ]
