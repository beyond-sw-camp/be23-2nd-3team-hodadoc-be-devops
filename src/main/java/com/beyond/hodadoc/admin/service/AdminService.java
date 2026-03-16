package com.beyond.hodadoc.admin.service;

import com.beyond.hodadoc.admin.domain.Department;
import com.beyond.hodadoc.admin.domain.Filter;
import com.beyond.hodadoc.admin.dtos.AdminSummaryDto;
import com.beyond.hodadoc.admin.dtos.FilterCreateDto;
import com.beyond.hodadoc.admin.dtos.NameDto;
import com.beyond.hodadoc.admin.repository.DepartmentRepository;
import com.beyond.hodadoc.admin.repository.FilterRepository;
import com.beyond.hodadoc.account.domain.Account;
import com.beyond.hodadoc.account.domain.Role;
import com.beyond.hodadoc.account.repository.AccountRepository;
import com.beyond.hodadoc.chat.domain.ChatParticipant;
import com.beyond.hodadoc.chat.repository.ChatParticipantRepository;
import com.beyond.hodadoc.chat.repository.ReadStatusRepository;
import com.beyond.hodadoc.hospital.domain.HospitalStatus;
import com.beyond.hodadoc.hospital.repository.HospitalFilterRepositoy;
import com.beyond.hodadoc.hospital.repository.HospitalRepository;
import com.beyond.hodadoc.review.domain.ReviewStatus;
import com.beyond.hodadoc.review.repository.ReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class AdminService {
    private final FilterRepository filterRepository;
    private final DepartmentRepository departmentRepository;
    private final HospitalRepository hospitalRepository;
    private final HospitalFilterRepositoy hospitalFilterRepository;
    private final ReviewRepository reviewRepository;
    private final AccountRepository accountRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ReadStatusRepository readStatusRepository;

    @Autowired
    public AdminService(DepartmentRepository departmentRepository,
                        FilterRepository filterRepository,
                        HospitalRepository hospitalRepository, HospitalFilterRepositoy hospitalFilterRepository,
                        ReviewRepository reviewRepository,
                        AccountRepository accountRepository,
                        ChatParticipantRepository chatParticipantRepository,
                        ReadStatusRepository readStatusRepository) {
        this.departmentRepository = departmentRepository;
        this.filterRepository = filterRepository;
        this.hospitalRepository = hospitalRepository;
        this.hospitalFilterRepository = hospitalFilterRepository;
        this.reviewRepository = reviewRepository;
        this.accountRepository = accountRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.readStatusRepository = readStatusRepository;
    }

    public AdminSummaryDto getDashboardSummary() {
        long pendingHospitalCount = hospitalRepository.countByStatus(HospitalStatus.PENDING);
        long reportedReviewCount = reviewRepository
                .findByStatusAndReportCountGreaterThan(ReviewStatus.REPORTED, 0, PageRequest.of(0, 1))
                .getTotalElements();

        // 채팅 미답변 통계: 관리자 기준 안 읽은 메시지가 있는 채팅방을 상대방 role별로 분류
        long patientUnanswered = 0;
        long hospitalUnanswered = 0;
        Account adminAccount = accountRepository.findFirstByRoleAndDelYn(Role.ADMIN, "N").orElse(null);
        if (adminAccount != null) {
            // 1. 관리자 채팅방 목록 (fetch join) - 쿼리 1개
            List<ChatParticipant> adminParticipants = chatParticipantRepository.findAllByAccountAndLeftYnWithChatRoom(adminAccount, "N");
            if (!adminParticipants.isEmpty()) {
                List<com.beyond.hodadoc.chat.domain.ChatRoom> adminRoomList = adminParticipants.stream()
                        .map(ChatParticipant::getChatRoom).collect(java.util.stream.Collectors.toList());

                // 2. 미읽음 카운트 한번에 조회 - 쿼리 1개
                List<Object[]> unreadCounts = readStatusRepository.countUnreadByChatRoomsAndAccount(adminRoomList, adminAccount);
                java.util.Map<Long, Long> unreadMap = unreadCounts.stream()
                        .collect(java.util.stream.Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

                // 3. 미읽음 있는 방의 참가자만 한번에 조회 - 쿼리 1개
                List<com.beyond.hodadoc.chat.domain.ChatRoom> unreadRooms = adminParticipants.stream()
                        .map(ChatParticipant::getChatRoom)
                        .filter(room -> unreadMap.getOrDefault(room.getId(), 0L) > 0)
                        .collect(java.util.stream.Collectors.toList());

                if (!unreadRooms.isEmpty()) {
                    List<ChatParticipant> allParticipants = chatParticipantRepository.findAllByChatRoomInWithAccount(unreadRooms);
                    for (ChatParticipant p : allParticipants) {
                        if (!p.getAccount().getId().equals(adminAccount.getId())) {
                            if (p.getAccount().getRole() == Role.PATIENT) {
                                patientUnanswered++;
                            } else if (p.getAccount().getRole() == Role.HOSPITAL_ADMIN) {
                                hospitalUnanswered++;
                            }
                        }
                    }
                }
            }
        }

        return AdminSummaryDto.builder()
                .pendingHospitalCount(pendingHospitalCount)
                .reportedReviewCount(reportedReviewCount)
                .patientUnansweredCount(patientUnanswered)
                .hospitalUnansweredCount(hospitalUnanswered)
                .build();
    }

    public Department createDepartment(NameDto dto) {
        Department department = Department.builder()
                .name(dto.getName())
                .build();
        departmentRepository.save(department);
        return department;
    }

    public List<Department> findAllDepartments() {
        return departmentRepository.findAll();
    }

    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("진료과를 찾을 수 없습니다."));
        departmentRepository.delete(department);
    }

    public Filter createFilter(FilterCreateDto dto) {
        Filter filter = Filter.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .build();
        filterRepository.save(filter);
        return filter;
    }

    public List<Filter> findAllFilters() {
        return filterRepository.findAll();
    }

    public void deleteFilter(Long id) {
        Filter filter = filterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("필터를 찾을 수 없습니다."));
        if (hospitalFilterRepository.existsByFilter(filter)) {
            throw new IllegalStateException("해당 필터를 사용 중인 병원이 있어 삭제할 수 없습니다.");
        }
        filterRepository.delete(filter);
    }
}
