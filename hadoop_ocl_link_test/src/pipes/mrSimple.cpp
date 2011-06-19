#include <string>

#include  <stdint.h>  // <-- this is missing from the book

#include "hadoop/Pipes.hh"
#include "hadoop/TemplateFactory.hh"

class MR_SimpleMapper : public HadoopPipes::Mapper
{
public:

  MR_SimpleMapper(HadoopPipes::TaskContext& context) {}

  void map(HadoopPipes::MapContext& context)
  {
    std::string line =  context.getInputValue();
    line.append("_map_called");
    context.emit(context.getInputKey(), line);
  }

};

class MR_SimpleReducer : public HadoopPipes::Reducer
{
public:

  MR_SimpleReducer(HadoopPipes::TaskContext& context) {}

  void reduce(HadoopPipes::ReduceContext& context)
  {
    std::string line;
    while(context.nextValue())
    {
    	line =  context.getInputValue();
    	line.append("_reduce_called");
    	context.emit(context.getInputKey(), line);
    }
  }

};

int main(int argc, char* argv[])
{
  return HadoopPipes::runTask(HadoopPipes::TemplateFactory<MR_SimpleMapper, MR_SimpleReducer>());
}
