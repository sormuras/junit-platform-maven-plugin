package mockitov5attach;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class App_Runner_Test {
  @Mock private AppInterface mockApp;
  @InjectMocks private AppRunner sut;

  @Test
  void checking_the_runner_if_the_app_is_running_should_invoke_the_underlying_app() {
    given(mockApp.running()).willReturn(true);
    assert sut.isAppRunning();
    then(mockApp).should().running();
  }
}
