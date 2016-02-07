<?php
/* settings.php */
$error = "";
include('user-rights.php');
include('sa-functions.php');
include('sa-login.php');
$is_logged_in = isset($_SESSION['login_user']);
?>

<html>
 <head>
  <title>
	${app.name} - Settings
  </title>
  <link rel="stylesheet" type="text/css" href="css/style.css">
 </head>
 <body>
<h1>
	${app.name} - Settings
</h1>
<?php
if ($is_logged_in) {
	$loggedInUser = $_SESSION['login_user'];
	$is_primary = isPrimaryByUsername($loggedInUser);
	if ($is_primary) {
		?>
		<div id="items">
		Logged as <?php echo $loggedInUser; ?>, <a href="sa-logout.php">Logout</a> | Settings | <a href="change-password.php">Change Password</a>
		<h2><a href="index.php">Home</a> > Settings</h2>
		<a href="user.php?page=create">Create New</a>
		<table>
			<thead>
				<tr>
					<td>Username</td>
					<td>Rolename</td>
					<td>Primary</td>
					<td>#</td>
				</tr>
			</thead>
			<tbody>
			<?php
			$users = getAllUsers();
			foreach ($users as $user) {
				?>
				<tr>
				  <td><?php echo $user["username"]; ?></td>
				  <td><?php echo $user["rolename"]; ?></td>
				  <td><?php echo $user["primary"]; ?></td>
				  <td>
				    <a href="user.php?page=edit&id=<?php echo $user['id']; ?>">edit</a> |
				    <a href="user.php?page=delete&id=<?php echo $user['id']; ?>">delete</a>
				  </td>
				</tr>
			<?php } ?>
			<tbody>
		</table>
		</div>
		<?php
	} else {
		?>
		You are not allowed to change settings.
		<?php
	}
} else {
	include('login_form.php');
}
?>
<div id="footer">
Generated by AppsoFluna
</div>
 </body>
</html>
