# docker-patch
create a diff between docker_img_A and docker_img_B, and generate a patch.
通过比对两个docker镜像的layer，生成一个更新补丁，实现docker镜像的增量发版，无法发布整个镜像。
