#!/bin/bash 
set -eu

valgrind --leak-check=full --show-leak-kinds=all  ./sobelgui -T=60 -c  
