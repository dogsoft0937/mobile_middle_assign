# Generated by Django 5.1.2 on 2024-10-24 07:08

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('blog', '0001_initial'),
    ]

    operations = [
        migrations.AddField(
            model_name='post',
            name='image',
            field=models.ImageField(default='', upload_to='blog_image/%Y/%m/%d/'),
            preserve_default=False,
        ),
    ]