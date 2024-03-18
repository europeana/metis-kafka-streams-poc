FROM  postgres:14-alpine

COPY init.sql /docker-entrypoint-initdb.d/

RUN echo -e "\nclientPort=5432" >>/docker-entrypoint.sh