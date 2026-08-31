package haitai.ht_ax_hackathon.service;

import haitai.ht_ax_hackathon.domain.Judge;
import haitai.ht_ax_hackathon.repository.JudgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JudgeUserDetailsService implements UserDetailsService {

    private final JudgeRepository judgeRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Judge judge = judgeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("심사자 계정을 찾을 수 없습니다."));

        return User.builder()
                .username(judge.getUsername())
                .password(judge.getPasswordHash())
                .disabled(!judge.isActive())
                .roles("JUDGE")
                .build();
    }
}
