#!/usr/bin/env bash

ORIG_OPTS="$JAVA_OPTS"
NUM_RUNS=5
DATA=~/data/query/tpch/SF0

resetOpts(){
  export JAVA_OPTS="$ORIG_OPTS"
}

addOpt(){
  export JAVA_OPTS="$1 $JAVA_OPTS"
}


configureOpts(){
  local app=$1
  local target=$2
  local threads=$3
  local extra=$4
  resetOpts
  addOpt -Dstats.dump 
  addOpt -Dstats.dump.component=app 
  addOpt -Dstats.dump.overwrite
  addOpt -Dstats.output.dir="$PWD/times"
  addOpt -Dstats.output.filename="${app}-${target}-${threads}-${extra}.times" 
}

runCommand(){
  echo -n $1
  shift
  local exe=$1
  shift
  $exe "$@" >/dev/null
  local res=$?
  if [ $res -eq 0 ]; then
    echo " => Success"
  else 
    echo " => Failed"
    exit 1
  fi 
}

executeForAllThreads(){
  local app=$1
  local runner=$2
  local extras=$3
  for target in scala; do ## cpp didn't work for all
    for numThreads in 1; do #2 4 8; do
      configureOpts $app $target $numThreads
      local threadOpt=""
      if [ $target = cpp ]; then
        threadOpt="-t 1 --cpp $numThreads"
      else 
        threadOpt="-t $numThreads"
      fi
      echo -n "Running $app on $numThreads threads (target=$target)"
      local logFile=$app-$target-$numThreads-$extras
      bin/delite $threadOpt -r $NUM_RUNS -v ${app}Compiler $DATA \
        > logs/out/$logFile 2> logs/err/$logFile
      local res=$?
      if [ $res -eq 0 ]; then
        echo " => Success"
      else 
        echo " => Failed"
        exit 1
      fi 
    done
  done
  echo ""
}

runApp(){
  local app=$1
  local runner=$2

  # Run without any optimizations
  rm -f $runner.deg
  runCommand "Compiling $app with (fusion=false,soa=false)" \
    "bin/delitec" --nf --ns "${app}Compiler"
  executeForAllThreads $app $runner "nf-ns" 

  # Run with loop fusion only 
  rm -f $runner.deg
  runCommand "Compiling $app with (fusion=true,soa=false)" \
    "bin/delitec" --ns "${app}Compiler"
  executeForAllThreads $app $runner "f-ns" 
  
  # Run with soa only
  rm -f $runner.deg
  runCommand "Compiling $app with (fusion=false,soa=true)" \
    "bin/delitec" --nf "${app}Compiler"
  executeForAllThreads $app $runner "nf-s" 

  # Run with both
  rm -f $runner.deg
  runCommand "Compiling $app with (fusion=true,soa=true)" \
    "bin/delitec" "${app}Compiler"
  executeForAllThreads $app $runner "f-s" 
  
}

main(){
  local starttime=$(date +%s)
  source init-env.sh
  #forge/bin/update ppl.dsl.forge.dsls.optiql.OptiQLDSLRunner OptiQL
  cd published/OptiQL

  mkdir -p logs/out
  mkdir -p logs/err
  mkdir -p "times"

  for idx in 1 6; do # 2 3 4 14 
    local app="TPCHQ$idx"
    local runner="ppl.apps.dataquery.tpch.$app"
    
    runApp $app $runner
  done

  local endtime=$(date +%s)
  local executionTime=$(echo "$endtime - $starttime" | bc)
  echo "Finished in $executionTime seconds"
}


# Run main
main
