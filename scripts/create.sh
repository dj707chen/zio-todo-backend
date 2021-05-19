#!/usr/bin/env bash

curl -d '{"title":"Roof repair invoice", "order":1}' -H 'Content-Type: application/json' http://localhost:8080/todos
