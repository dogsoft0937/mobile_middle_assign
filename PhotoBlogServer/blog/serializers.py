from blog.models import Post
from rest_framework import serializers
from django.contrib.auth.models import User

class PostSerializer(serializers.HyperlinkedModelSerializer):
    author = serializers.PrimaryKeyRelatedField(queryset=User.objects.all())
    
    class Meta:
        model = Post
        fields = ('author', 'title', 'text', 'image')  # created_date, published_date 제외

    def create(self, validated_data):
        # Post 객체를 생성하기 전에 받은 데이터 출력
        print("Received Data:", validated_data)
        
        # Post 객체 생성
        post = Post.objects.create(**validated_data)
        
        # 생성된 객체 반환
        return post