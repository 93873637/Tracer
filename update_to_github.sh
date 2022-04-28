#!/bin/bash 

REPO_NAME=Tracer.git

TIME_FLAG=`date +"%y%m%d.%H%M%S"`

run_git_cmd(){
    #echo "run_git_cmd..."
    echo ""
    echo "# $*"
    $*
    echo ""
}

echo ""
echo "***update_to_github: ${REPO_NAME}: enter..."
echo ""

if [ ! -d ".git" ]; then
    echo "***.git directory not exist, init..."
    git init
    git remote add origin git@github.com:93873637/${REPO_NAME}
    git pull origin master
else
    echo "***find .git directory for ${REPO_NAME}"
fi

run_git_cmd "git status"
run_git_cmd "git add ."
run_git_cmd "git commit -m update_${TIME_FLAG}"
run_git_cmd "git push git@github.com:93873637/${REPO_NAME}"
run_git_cmd "git status"
run_git_cmd "git log"

echo ""
echo "***update_to_github: ${REPO_NAME}, exit."
echo ""

sleep 5s
