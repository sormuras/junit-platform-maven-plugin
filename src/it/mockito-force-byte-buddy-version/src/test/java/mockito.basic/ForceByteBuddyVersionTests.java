package mockito.basic;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BasicTests {

  private final List<Integer> sharedList;

  BasicTests(@Mock List<Integer> sharedList) {
    this.sharedList = sharedList;
  }

  @Test
  void shouldDoSomething() {
    sharedList.add(100);
  }
}
