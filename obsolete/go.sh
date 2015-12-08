#!/usr/bin/env bash

echo "Running all tests..."
lein midje &> tests.log

if [ $? -ne 0 ]
then
  echo "Tests failed!"
  cat ./tests.log
else
  echo "Tests Passed"
fi

