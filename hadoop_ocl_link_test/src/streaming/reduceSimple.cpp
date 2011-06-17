#include <iostream>

int main() {
  std::string input;
  while(getline(std::cin, input))
  {
    input.append("_reduce_called");
    std::cout << input << std::endl;
  }
  return EXIT_SUCCESS;
}
