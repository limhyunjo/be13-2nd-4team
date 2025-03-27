package com.beyond.backend.controller;

import com.beyond.backend.domain.post.dto.PostDto;
import com.beyond.backend.domain.post.dto.PostResponseDto;
import com.beyond.backend.domain.post.dto.UserPostResponseDto;
import com.beyond.backend.domain.post.entity.BoardType;
import com.beyond.backend.domain.post.entity.PostSearchOption;
import com.beyond.backend.domain.post.entity.PostSortOption;
import com.beyond.backend.domain.post.entity.PostStatus;
import com.beyond.backend.domain.post.service.BookMarkService;
import com.beyond.backend.domain.post.service.PostService;
import com.beyond.backend.domain.user.dto.CustomUserDetails;
import com.beyond.backend.domain.user.service.AdminService;
import com.beyond.backend.domain.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *
 * <p>packageName    : com.beyond.backend.controller
 * <p>fileName       : PostController
 * <p>author         : hyunjo
 * <p>date           : 25. 2. 2.
 * <p>description    :
 */
/*
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 2.        hyunjo             최초 생성
 * 25. 2. 17.       hyunjo             내용 수정
 * 25. 2. 20.       hyunjo             내용 수정
 */
@Tag(name = "03 게시판 API", description = "게시판 API")
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final BookMarkService bookMarkService;
    private final AdminService adminService;
    

    // 게시글 단건 조회 (활성화된 게시글만 조회 가능/ 관리자와 작성자만 보임)
    @Operation(summary = "게시글 단건 조회", description = "활성화된 게시글 상세 조회")
    @GetMapping("/posts/{postNo}/with-comments")
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#postNo, 'POST_ACCESS')")
    public ResponseEntity<UserPostResponseDto> getPostDetail(
            @PathVariable Long postNo,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {

        postService.viewPost(userDetails, postNo, request);
        UserPostResponseDto userPostResponseDto = postService.getPostById(postNo);
        return ResponseEntity.ok(userPostResponseDto);
    }



    // 게시글 전체 조회 (아무 조건이 없는 경우 기본 최신순으로 조회됨)
    @Operation(summary = "게시글 검색 및 전체 조회", description = "제목, 내용, 작성자에서 검색어가 포함된 게시글 조회(검색이 아닌 경우 전체 조회)")
    @GetMapping("/posts")
    public ResponseEntity<?> searchPosts(
            @RequestParam BoardType boardType,
            @RequestParam(required = false) PostSortOption postSortOption,
            @RequestParam(required = false) PostSearchOption option,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, page= 0)Pageable pageable) {
          // 검색어가 있는 경우 검색 조건도 무조건 선택하도록 뒤에서 처리함


        Page<PostResponseDto> searchPosts = postService.searchPosts(boardType, option, postSortOption, keyword, pageable);

        return ResponseEntity.ok(searchPosts);
    }


    //게시글 생성
    @Operation(summary = "게시글 등록", description = "게시글 등록 ")
    @PostMapping("/posts")
    public ResponseEntity<PostResponseDto> createPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PostDto postDto) {

        //게시글 작성할 보드 타입을 지정해야 함

        // 게시글이 notice인 경우 관리자인지 검증
        if(postDto.getBoardType() == BoardType.NOTICE){
        adminService.validateAdmin( userDetails.getUser().getUsername());
        }

        PostResponseDto postResponseDto = postService.createPost(postDto.getBoardType(), postDto, userDetails.getUser().getNo());
        return ResponseEntity.ok(postResponseDto);
    }

    // 게시글 수정
    @Operation(summary = "게시글 수정", description = "제목, 내용, 게시글 상태 수정 가능")
    @PostMapping("/posts/{postNo}")
    @PreAuthorize("hasPermission(#postNo, 'POST')")
    public ResponseEntity<PostResponseDto> updatePost(
            // 게시글 생성 시에는 무조건 활성 상태로 만들어지고 게시글 수정 시 활성/비활성 가능
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postNo,
            @Valid @RequestBody PostDto postDto){

        // 게시글이 notice인 경우 관리자인지 검증
        if(postDto.getBoardType() == BoardType.NOTICE){
            adminService.validateAdmin( userDetails.getUser().getUsername());
        }
        PostResponseDto updatePost = postService.updatePost(postNo, postDto);
        return ResponseEntity.ok(updatePost);
    }
    

   
    // 게시글 삭제 (작성자,관리자)

    //@PreAuthorize("@postPermissionEvaluator.isOwner(#postNo, authentication)")
    @Operation(summary = "게시글 삭제", description = "게시글 삭제")
    @DeleteMapping( "/posts/{postNo}")
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#postNo, 'POST')")
    public ResponseEntity<String> deletePost(
            @PathVariable Long postNo) {

        // 게시글 삭제 시 댓글도 자동으로 삭제됨
        postService.deletePost(postNo);
        return ResponseEntity.status(HttpStatus.OK).body("게시물이 삭제되었습니다.");
    }


    // 유저가 작성한 게시글 전체 조회 (비활성화 상태인 게시글도 보임)
    @Operation(summary = "유저가 작성한 게시글 전체 조회", description = "개인 페이지에서 자신의 게시글 전체 조회")
    @GetMapping("/user-page/posts")
    public ResponseEntity<Page<UserPostResponseDto>> getMyPost(
            // 내가 쓴 게시글을 게시판 종류를 나눠서 볼 수 있음
            @RequestParam BoardType boardType,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, page= 0)Pageable pageable){

        Page<UserPostResponseDto> userPosts = postService.getUserPosts(boardType, pageable, userDetails.getUser().getNo());

        return ResponseEntity.ok(userPosts);
    }


    // 게시글 북마크 추가 / 취소
    @Operation(summary = "게시글 북마크 추가 및 취소", description = "게시글 북마크 상태 (추가, 취소)")
    @PostMapping("/posts/{postNo}/bookmark")
    public ResponseEntity<String> checkBookMark(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postNo) {

        String bookMarkResult = bookMarkService.checkBookMark(postNo, userDetails.getUser().getNo());
        return ResponseEntity.ok(bookMarkResult);
    }


    // 유저가 북마크한 게시글 전체 조회
    @Operation(summary = "북마크한 게시글 조회", description = "유저가 북마크한 게시글을 게시판 타입별로 조회. 타입이 없으면 전체 조회.")
    @GetMapping("/user-page/bookmark")
    public ResponseEntity<Page<UserPostResponseDto>> getBookmarkedPosts(
            @RequestParam(required = false) BoardType boardType,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Page<UserPostResponseDto> result = bookMarkService.getBookmarkedPosts(boardType, pageable, userDetails.getUser().getNo());
        return ResponseEntity.ok(result);
    }
}

